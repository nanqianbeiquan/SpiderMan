package gs;

import java.sql.BatchUpdateException;

import tools.SysConfig;
import tools.UpdateJob;

public class LnGsUpdateJob extends UpdateJob{

//	public SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss -> ");
	
	public LnGsUpdateJob() throws Exception
	{
		super();
		setProvName("辽宁省");
		setProcessName("LnGs");
		setCodeTable("GsSrc");
		setCodeCol("orgName");
		setBatchSize(10);
		setSearcher(new LnGsSearcherHeadless());
	}
	
	public int updateCode(String orgName) throws Exception
	{
		logger.info("updating "+orgName);
		int processStatus=0;
		GsModel model =null;
		for(int i=0;i<SysConfig.MAX_TRY_TIMES;i++)
		{
			model = (GsModel) searcher.search(orgName);
			logger.info(orgName+":"+model.updateStatus);
			if(model.updateStatus==0 || model.updateStatus==1 || model.updateStatus==8)
			{
				break;
			}
			else
			{
//				dbClient.clearBatch();
				searcher.quitDriver();
				searcher.initDriver();
				continue;
			}
		}
		if(model.updateStatus==0 || model.updateStatus==1 || model.updateStatus==8)
		{
			String updateCmd="update GsSrc set "+model+" where orgName='"+orgName+"'";
//			dbClient.addBatch(updateCmd);
//			dbClient.executeBatch();
			dbClient.execute(updateCmd);
//			dbClient.commit();
		}
		else
		{
			processStatus=1;
			logger.info("失败次数超过10次，退出程序。");
		}
		return processStatus;
	
	}
	
	public static void main(String[] args) throws Exception
	{
//		new LnGsUpdateJob().run();
	}
}
