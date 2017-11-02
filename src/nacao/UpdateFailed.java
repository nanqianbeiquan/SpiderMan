package nacao;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import tools.JobConfig;
import tools.Logger;
import tools.MSSQLClient;
import tools.SysConfig;

public class UpdateFailed {

	public String dstTableName;
	public MSSQLClient dbClient;
	public NacaoOrgSearcherTest searcher;
	public Logger logger;
	public String host="localhost";
	public UpdateFailed() throws ClassNotFoundException, SQLException, IOException
	{
		dbClient=new MSSQLClient(
				String.format("jdbc:sqlserver://%s:1433;DatabaseName=%s",SysConfig.MSSQL_HOST,SysConfig.MSSQL_DB),
				SysConfig.MSSQL_USER, //user
				SysConfig.MSSQL_PWD, //pwd
				false //autoCommit
				);
		logger=new Logger("UpdateFailed");
	}
	
	public void initSearcher() throws Exception
	{
		searcher=new NacaoOrgSearcherTest();
		searcher.setLogger(logger);
		searcher.initDriver();
	}
	
	public void setDstTableName(String tableName)
	{
		dstTableName=tableName;
	}
	
	public void update() throws Exception
	{
		String sql0="select top 1 orgCode,updateStatus from NacaoOrg where updateStatus not in (0,1)";
		ResultSet res0 = dbClient.execute(sql0);
		if(res0.next()==true)
		{
			initSearcher();
			String sql1="select orgCode,updateStatus from NacaoOrg where updateStatus not in (0,1)";
			ResultSet res1 = dbClient.execute(sql1);

			while(res1.next()==true)
			{
				String orgCode=res1.getString(1);
//				float updateStatus=res1.getFloat(2);
				
				NACAO nacao=searcher.search(orgCode);
				
				while (nacao.updateStatus!=0 && nacao.updateStatus!=1)
				{
					nacao=searcher.search(orgCode);
				}
				
				
				String[] colsAndVals=nacao.getColsAndVals();
				colsAndVals[0]+=",lastUpdateTime,host";
				colsAndVals[1]+=",getDate(),"+"'"+host+"'";
				
				String deleteCmd=String.format("delete from %s where orgCode='%s'",dstTableName,orgCode);
				String insertCmd=String.format("insert into %s(%s) values(%s)",dstTableName,colsAndVals[0],colsAndVals[1]);
				
				System.out.println(deleteCmd);
				System.out.println(insertCmd);
				
				dbClient.execute(deleteCmd);
				dbClient.execute(insertCmd);
				dbClient.commit();
			}
		}
		
		
	}
	
	public static void run(JobConfig jobConf) throws Exception
	{
		UpdateFailed job=new UpdateFailed();
		job.setDstTableName(jobConf.getString("dstTableName"));
		job.update();
	}
	
	public static void main(String[] args) throws Exception
	{
		UpdateFailed job=new UpdateFailed();
		job.setDstTableName("NacaoOrg");
		
		job.update();
	}
}
