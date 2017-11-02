package tools;


import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class UpdateJob {

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
	public String signNewTemplate;
	public String provName=null;
	public ArrayList<String> batchList=new ArrayList<String>();
	public static String firefoxPath=null;
	
	public UpdateJob() throws IOException, ClassNotFoundException, SQLException
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
		signNewTemplate=
				"update top(%d) %s "
				+ "set updateStatus=-2, processIdentity='%d_%d',lastUpdateTime=getDate() "
				+"where province='%s' and updateStatus=-1";
		System.out.println(signNewTemplate);
		registerProcess();
		logger=new Logger(processName+"_"+processIdentity);
		initSearcher();
		execUpdateProc();
	}

	public void execUpdateProc() throws Exception
	{
		logger.info("execUpdateProc...");
		for(int batchIdx=0;batchIdx<Integer.MAX_VALUE;batchIdx++)
		{
			int rowCount=0;
			int processStatus=0;
			
			String sql1=String.format(
					signNewTemplate,
					batchSize,codeTable,processIdentity,batchIdx,provName);
					
			dbClient.execute(sql1);
			dbClient.commit();

			String sql2=String.format("select %s from %s "
					+ "where processIdentity='%d_%d'",codeCol,codeTable,processIdentity,batchIdx);
			ResultSet res2=dbClient.execute(sql2);
			while(res2.next()==true)
			{
				batchList.add(res2.getString(1));
			}
			res2.close();
			if(batchList.size()==0)
			{
				String sql3=String.format("update %s set updateStatus=-1 where province='%s' and updateStatus=-2 "
						+ "and DATEDIFF(MINUTE,lastUpdateTime,GETDATE())>=10",codeTable,provName);
				dbClient.execute(sql3);
				ResultSet rowCountRes = dbClient.execute("select @@ROWCOUNT");
				dbClient.commit();
				if(rowCountRes.next())
				{
					rowCount=rowCountRes.getInt(1);
				}
				if(rowCount==0)
				{
					logger.info("更新完毕！");
					break;
				}
				
			}
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
			if(processStatus!=0)
			{
				break;
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
