package tools;


import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class UpdateJob2 {

	public MSSQLClient dbClient;	
	public static String processID;
	public int totalUpdateCnt=0;
	public Searcher searcher;
	public int batchSize;
	public Logger logger;
	public String codeTable;
	public String codeCol;
	public String processTable="ProcessStatus";
	public String processName;
	public String host;
	public String hostname;
	public String hostType="PC"; //终端类型
	public static int processIdentity;
	public String signNewTemplate=
			"update top(%d) %s "
			+ "set updateStatus=-2, processIdentity=%d,lastUpdateTime=getDate() "
			+"where updateStatus=-1";
	public String provName=null;
	
	public ArrayList<String> batchList=new ArrayList<String>();
	public static String firefoxPath=null;
	
	public UpdateJob2() throws IOException, ClassNotFoundException, SQLException
	{
		dbClient=new MSSQLClient(
				String.format("jdbc:sqlserver://%s:1433;DatabaseName=%s",SysConfig.MSSQL_HOST,SysConfig.MSSQL_DB),
				SysConfig.MSSQL_USER, //user
				SysConfig.MSSQL_PWD, //pwd
				false //autoCommit
				);
	}
	
	public void setProvName(String prov)
	{
		this.provName=prov;
		
	}
	public void registerProcess() throws SQLException
	{
		String curTime=SysConfig.getCurTime();
		String sql1=String.format(
				"insert into %s(processID,processName,processStatus,startTime,lastUpdateTime,totalUpdateCnt,host,hostType) "
				+ "values('%s','%s',0,'%s',getDate(),0,'%s','%s')", processTable,processID,processName,curTime,host,hostType);
//		System.out.println(sql1);
		dbClient.statement.executeUpdate(sql1, Statement.RETURN_GENERATED_KEYS);
		ResultSet identityRes = dbClient.statement.getGeneratedKeys();
		dbClient.commit();
		if(identityRes.next())
		{
			processIdentity=identityRes.getInt(1);
		}
		System.out.println("processIdentity:"+processIdentity);
	}
	
	public void run(JobConfig jobConf) throws Exception
	{
		processName=jobConf.jobName;
		processID=ProcessInfo.getPID();
		host=SysConfig.getLocalHost();
		HashMap<String, String> configArgs = getConfigArgs();
		
		if(provName!=null)
		{
			signNewTemplate=signNewTemplate+" and province='"+provName+"'";
		}
		
		if(configArgs.containsKey("startCode") && configArgs.containsKey("stopCode"))
		{
			String startCode=configArgs.get("startCode");
			String stopCode=configArgs.get("stopCode");
			signNewTemplate=signNewTemplate.replace("updateStatus=-1", codeCol+" between '"+startCode+"' and '"+stopCode+"' and updateStatus=-1");
		}
		else if(!configArgs.containsKey("startCode") && configArgs.containsKey("stopCode"))
		{
			String stopCode=configArgs.get("stopCode");
			signNewTemplate=signNewTemplate.replace("updateStatus=-1", codeCol+" <= '"+stopCode+"' and updateStatus=-1");
		}
		else if(configArgs.containsKey("startCode") && !configArgs.containsKey("stopCode"))
		{
			String startCode=configArgs.get("startCode");
			signNewTemplate=signNewTemplate.replace("updateStatus=-1", codeCol+" >= '"+startCode+"' and updateStatus=-1");
		}
		System.out.println(signNewTemplate);
		if (jobConf.hasProperty("firefoxPath"))
		{
			firefoxPath=jobConf.getString("firefoxPath");
		}
		registerProcess();
		logger=new Logger(processName+"_"+processIdentity);
		initSearcher();
		execUpdateProc();
	}

	public void execUpdateProc() throws Exception
	{
		logger.info("execUpdateProc...");
		while(true)
		{
			int failedIdentity=-1;
			int rowCount=0;
			int processStatus=0;
			
			String sql0=String.format("update top(1) %s set processStatus=9,takeoverIdentity=%d where processStatus!=0 and processStatus!=9 and processName='%s'", processTable,processIdentity,processName);
			dbClient.statement.executeUpdate(sql0);
			dbClient.commit();
			ResultSet failedIdentityRes = dbClient.execute(String.format("select processIdentity from %s where takeoverIdentity=%d and processStatus=9",processTable,processIdentity));
			if(failedIdentityRes.next())
			{
				failedIdentity=failedIdentityRes.getInt(1);
			}
			failedIdentityRes.close();

			if(failedIdentity!=-1)
			{
				String sql1=String.format("update %s set takeoverIdentity=null where processIdentity=%d", processTable,failedIdentity);
				dbClient.execute(sql1);
				dbClient.commit();
				
				String sql2=String.format("update %s set lastUpdateTime=getDate() "
						+ ",processIdentity=%d "
						+ "where processIdentity=%d and updateStatus=-2",codeTable,processIdentity,failedIdentity);
				dbClient.execute(sql2);
				ResultSet rowCountRes = dbClient.execute("select @@ROWCOUNT");
				dbClient.commit();
				while(rowCountRes.next())
				{
					rowCount=rowCountRes.getInt(1);
				}
				rowCountRes.close();
			}
			if(rowCount==0)
			{
				String sql3=String.format(
						signNewTemplate,
						batchSize,codeTable,processIdentity);
						
				dbClient.execute(sql3);
				dbClient.commit();
			}

			String sql4=String.format("select %s from %s "
					+ "where processIdentity=%d and updateStatus=-2 order by reverse(%s) ",codeCol,codeTable,processIdentity,codeCol);
			ResultSet res4=dbClient.execute(sql4);
			while(res4.next()==true)
			{
				batchList.add(res4.getString(1));
			}
			res4.close();
			dbClient.commit();
			
			if(failedIdentity==-1 && batchList.isEmpty())
			{
				logger.info("Update complete!");
				break;
			}
			
			if(batchList.size()>0)
			{
				for(String code:batchList)
				{
					int updateResult=updateCode(code);
					if(updateResult!=0) 
					{
						processStatus=updateResult;
					}
					else
					{
						totalUpdateCnt++;
					}
					String updateProcessStatusCmd=String.format("update %s set processStatus='%d',lastUpdateTime=getDate(),totalUpdateCnt=%d "
							+ "where processIdentity=%d",
							processTable,processStatus,totalUpdateCnt,processIdentity);
					dbClient.execute(updateProcessStatusCmd);
					dbClient.commit();
					
					if(processStatus!=0)
					{
						break;
					}
					
				}
				batchList.clear();
			}
			if(processStatus!=0)
			{
				searcher.quitDriver();
				logger.close();
				System.exit(0);
			}
		}
		
		dbClient.execute(String.format("update %s set processStatus='9',lastUpdateTime=getDate() where processIdentity=%d",
				processTable,processIdentity));
		dbClient.commit();
		dbClient.close();
		searcher.quitDriver();
		logger.close();
		System.exit(0);
	}
	
	public HashMap<String,String> getConfigArgs() throws ClassNotFoundException, SQLException
	{
		HashMap<String,String> configArgs=new HashMap<String,String>();
		ResultSet res = dbClient.execute(String.format("select configArgs from JobConfig where jobName='%s'",processName));
		if(res.next())
		{
			String configText=res.getString("configArgs");
			configText=configText.substring(1, configText.length()-1);
			String[] kvArr = configText.split(",");
			for(String kv:kvArr)	
			{
				String k=kv.split("=")[0];
				String v=kv.split("=")[1];
				configArgs.put(k, v);
			}
			System.out.println(configArgs);
		}
		
		return configArgs;
	}
	
	public abstract int updateCode(String code) throws Exception;
	public void setSearcher(Searcher searcher)
	{
		this.searcher=searcher;
	}
	public void setProcessName(String processName)
	{
		this.processName=processName;
	}
	public void setCodeTable(String codeTable)
	{
		this.codeTable=codeTable;
	}
	public void setCodeCol(String col)
	{
		this.codeCol=col;
	}
	public void setBatchSize(int batchSize)
	{
		this.batchSize=batchSize;
	}
	
	public void initSearcher() throws Exception
	{
		searcher.setLogger(logger);
		searcher.setDbClient(dbClient);
		searcher.setProcessIdentity(processIdentity);
		int initSearcherRes=searcher.initDriver();
		if(initSearcherRes!=0)
		{
			logger.info("initialize searcher failed!");
			searcher.quitDriver();
			int processStatus=1;
			if(searcher.IpForbidden)
			{
				processStatus=4;
			}
			dbClient.execute(String.format("update %s set processStatus=%d,lastUpdateTime=getDate() where processIdentity=%d",
					processTable,processStatus,processIdentity));
			dbClient.commit();
			dbClient.close();
			logger.info("Searcher 初始化失败，退出程序!");
			searcher.quitDriver();
			logger.close();
			System.exit(0);
		}
	}

	
}
