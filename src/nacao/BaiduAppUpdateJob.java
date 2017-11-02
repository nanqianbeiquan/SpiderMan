package nacao;

import java.io.IOException;
import java.sql.SQLException;

import tools.SysConfig;
import tools.UpdateJob;

public class BaiduAppUpdateJob extends UpdateJob{
	
	public BaiduAppUpdateJob() throws ClassNotFoundException, IOException, SQLException
	{
		super();
//		setProcessName("BaiduApp");
		setCodeTable("ZZJGDM_ALL");
		setCodeCol("code");
//		setUpdateStatusCol("extract_Status");
		setBatchSize(30);
		setSearcher(new BaiduAppSearcher());
	}

//	private void setSearcher(NacaoOrgSearcher nacaoOrgSearcher) {
//		// TODO Auto-generated method stub
//	}

	@Override
	public int updateCode(String orgCode) throws Exception {
		// TODO Auto-generated method stub
		logger.info("查询代码："+orgCode);
		int processStatus=0;
		NACAO model=null;
		for(int i=0;i<SysConfig.MAX_TRY_TIMES;i++)
		{
			model = (NACAO) searcher.search(orgCode);
			logger.info("查询结果："+model.updateStatus);
			if(model.updateStatus==0 || model.updateStatus==1 || model.updateStatus==4)
			{
				break;
			}
			
		}
		if(model.updateStatus==0 || model.updateStatus==1)
		{
			String colsAndVals=model.toString();
			colsAndVals+=",lastUpdateTime=getDate()";

			String updateSql=String.format("update "+codeTable+" set %s where Code='%s'",colsAndVals,orgCode);
//			dbClient.addBatch(updateSql);
			dbClient.execute(updateSql);
			dbClient.commit();
//			logger.info("orgCode:"+orgCode+",updateStatus:"+model.updateStatus);
		}
		else if(model.updateStatus==4)
		{
			logger.info("您所在的IP暂时不能进行检索...");
			processStatus=4;
		}
		else
		{
			processStatus=1;
			logger.info("失败次数超过10次，退出程序。");
		}
		return processStatus;
	}

}
