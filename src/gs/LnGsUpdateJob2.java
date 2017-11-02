package gs;

import java.lang.management.ManagementFactory;
import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import tools.JobConfig;
import tools.Logger;
import tools.MSSQLClient;
import tools.SysConfig;

public class LnGsUpdateJob2 {

	public MSSQLClient dbClient;	
	public int batchSize=30;
	public int startBaseCode;
	public int stopBaseCode;
	public Logger logger;
	public String codeTable="GsSrc";
	
	public HashSet<String> batchSet=new HashSet<String>();
	public LnGsSearcherHeadless searcher=new LnGsSearcherHeadless();
	
	public String processTable="ProcessStatus";
	public static String processID;
	public static String hostname;
	public static String processName="LnGs";
	
	public static String host=null; //当前任务的IP
	public static String hostType="PC"; //终端类型
	public int totalUpdateCnt=0;	
	public int processIdentity;
	public SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss -> ");
	
	public LnGsUpdateJob2() throws Exception
	{
		dbClient=new MSSQLClient(
				String.format("jdbc:sqlserver://%s:1433;DatabaseName=%s",SysConfig.MSSQL_HOST,SysConfig.MSSQL_DB),
				SysConfig.MSSQL_USER, //user
				SysConfig.MSSQL_PWD, //pwd
				false //autoCommit
				);		
		registerProcess();
		logger=new Logger(processName+"_"+processIdentity);
	}
	
	public void registerProcess() throws Exception
	{
		String sql1=String.format(
				"insert into %s(processID,processName,processStatus,startTime,lastUpdateTime,totalUpdateCnt,host,hostType) "
				+ "values('%s','%s',0,getDate(),getDate(),0,'%s','%s')", processTable,processID,processName,host,hostType);
		dbClient.statement.executeUpdate(sql1, Statement.RETURN_GENERATED_KEYS);
		ResultSet identityRes = dbClient.statement.getGeneratedKeys();
		dbClient.commit();
		if(identityRes.next())
		{
			processIdentity=identityRes.getInt(1);
		}
		System.out.println("processIdentity:"+processIdentity);
	}
	
	public void run() throws Exception
	{
		while(true)
		{
			int failedIdentity=-1;
			int rowCount=0;
			String sql0=String.format(
					"select top 1 processIdentity from %s(tablockx) where processStatus=1 and processName='%s'",
					processTable,processName);
			ResultSet res0= dbClient.execute(sql0);
			if(res0.next()==true && res0.getObject(1)!=null)
			{
				failedIdentity=res0.getInt(1);
				String sql1=String.format("update %s set processStatus=9 where processIdentity=%d", processTable,failedIdentity);
				dbClient.execute(sql1);
			}
			res0.close();
			dbClient.commit();
			if(failedIdentity!=-1)
			{
				String sql2=String.format("update %s set lastUpdateTime=getDate() "
						+ ",processIdentity=%d "
						+ "where processIdentity=%d and updateStatus=-1",codeTable,processIdentity,failedIdentity);
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
				String sql3=String.format("update %s "
						+ "set updateStatus=-1, processIdentity=%d,lastUpdateTime=getDate() "
						+ "where registeredCode in "
						+ "(select top %d registeredCode from %s where updateStatus is null)",
						codeTable,processIdentity,batchSize,codeTable);
				dbClient.execute(sql3);
				dbClient.commit();
			}
			
			String sql4=String.format("select registeredCode from %s(tablockx) "
					+ "where processIdentity=%d and updateStatus=-1",codeTable,processIdentity);
			ResultSet res4=dbClient.execute(sql4);
			while(res4.next()==true)
			{
				batchSet.add(res4.getString(1));
			}
			res4.close();
			dbClient.commit();
			
			if(batchSet.size()>0)
			{
				for(String registeredCode:batchSet)
				{
					totalUpdateCnt++;
					update(registeredCode);
				}
				batchSet.clear();
			}
			else
			{
				logger.info("Update complete!");
				break;
			}			
		}
		
		dbClient.execute(String.format("update %s set processStatus='9' where processIdentity=%d",
				processTable,processIdentity));
		dbClient.commit();
		dbClient.close();
		searcher.quitDriver();
		logger.close();
		System.exit(0);
	}
	
	public void initSearcher() throws Exception
	{
		searcher.setLogger(logger);
		searcher.setDbClient(dbClient);
		searcher.initDriver();
	}
	
	public static void run(JobConfig jobConf) throws Exception
	{
		String processName = ManagementFactory.getRuntimeMXBean().getName();
		processID=processName.split("@")[0];
		hostname=processName.split("@")[1];
		if(jobConf.hasProperty("hostType"))
		{
			hostType=jobConf.getString("hostType");
		}
		if(hostType.equals("server"))
		{
			host=jobConf.getString("host");
		}
		else
		{
			host=jobConf.hasProperty("host")?jobConf.getString("host"):hostname;
		}
		LnGsUpdateJob2 job=new LnGsUpdateJob2();		
		job.initSearcher();
		job.run();
	}
	
	public void update(String registeredCode) throws Exception
	{
		logger.info("updating "+registeredCode);
		dbClient.statement.clearBatch();
		int processStatus=0;
		GsModel model =null;
		try
		{
			for(int i=0;i<SysConfig.MAX_TRY_TIMES;i++)
			{
				model = searcher.search(registeredCode);
				logger.info(registeredCode+" -> "+model.updateStatus);
				System.out.println(df.format(new Date())+registeredCode+":"+model.updateStatus);
				if(model.updateStatus==0 || model.updateStatus==1 || model.updateStatus==8)
				{
					break;
				}
//				else if(model.updateStatus==8)
//				{
//					logger.info("模板异常，请更新GsTables.xml。");
//					throw new Exception("网页模板异常!");
//				}
				else
				{
					dbClient.statement.clearBatch();
					searcher.quitDriver();
					searcher.initDriver();
					continue;
				}
			}
		}
		catch (Exception e)
		{
			logger.info(SysConfig.getError(e));
			processStatus=1;
		}
		if(model.updateStatus==0 || model.updateStatus==1 || model.updateStatus==8)
		{
			String updateCmd="update GsSrc set "+model+" where registeredCode='"+registeredCode+"'";
			dbClient.statement.addBatch(updateCmd);
		}
//		else if(model.updateStatus==8)
//		{
//			dbClient.statement.clearBatch();
//			processStatus=1;
//		}
		else
		{
			dbClient.statement.clearBatch();
			processStatus=1;
			logger.info("失败次数超过10次，退出程序。");
		}
		String updateProcessStatus=String.format("update %s set processStatus='%d',lastUpdateTime=getDate(),totalUpdateCnt=%d "
				+ "where processIdentity=%d",
				processTable,processStatus,totalUpdateCnt,processIdentity);
//		System.out.println(updateProcessStatus);
		
		dbClient.statement.addBatch(updateProcessStatus);
		dbClient.statement.executeBatch();
		try
		{
			dbClient.commit();
		}
		catch (BatchUpdateException e)
		{
			logger.info(SysConfig.getError(e));
			logger.info(e.getSQLState());
			processStatus=1;
		}
		
		
		if(processStatus==1)
		{
			dbClient.close();
			logger.close();
			System.exit(1);
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		String processName = ManagementFactory.getRuntimeMXBean().getName();
		String pid=processName.split("@")[0];
		String hostname=processName.split("@")[1];
		System.out.println(pid);
		System.out.println(hostname);
//		LnGsUpdateJob job=new LnGsUpdateJob("localhost");
//		job.initSearcher("default");
//		job.run();
	}
}
