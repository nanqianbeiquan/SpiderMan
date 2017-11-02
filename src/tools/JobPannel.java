package tools;

import sifa.SiFaStep1;
import sifa.SiFaStep2;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import gs.LnGsUpdateJob;
import nacao.BaiduAppSearcher;
import nacao.BaiduAppUpdateJob;
import nacao.NACAO;
import nacao.NacaoOrgUpdateJob;

public class JobPannel {

	
	public static void run(String[] args) throws Exception
	{
		JobConfig jobConf=new JobConfig(args);
		if(jobConf.jobName.equals("BaiduApp"))
		{
			new BaiduAppUpdateJob().run(jobConf);
		}
		else if(jobConf.jobName.equals("NacaoOrg"))
		{
			new NacaoOrgUpdateJob().run(jobConf);
		}
		else if(jobConf.jobName.equals("LnGs"))
		{
			new LnGsUpdateJob().run(jobConf);
		}
		else if(jobConf.jobName.equals("ConnectNetWork"))
		{
			ConnectNetWork.reconnect();
		}
		else if(jobConf.jobName.equals("SiFaStep2"))
		{
			new SiFaStep2().execUpdateProc();
		}
		else if(jobConf.jobName.equals("SiFaStep1"))
		{
			new SiFaStep1().run(jobConf);
		}
		else if(jobConf.jobName.equals("SiFaRollBack"))
		{
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
			String startDay="2016-02-25";
			while(startDay.compareTo("2016-03-18")<=0)
			{
				System.out.println(startDay);
				Calendar calendar = new GregorianCalendar();
		        calendar.setTime(sdf.parse(startDay));
		        calendar.add(Calendar.DATE,1);
		        startDay = sdf.format(calendar.getTime());   
		        JobConfig jobConfig=new JobConfig(new String[]{"--jobName=SiFaStep1","--uploadDay="+startDay});
		        new SiFaStep1().run(jobConfig);
			}
		}
		else
		{
			System.out.println("Please input right jobName!");
		}
	}
	
	public static void main(String[] args) throws Exception
	{
//		args=new String[]{"--jobName=LnGsUpdateJob"};
//		args=new String[]{"--jobName=NacaoOrg"};
//		args=new String[]{"--jobName=BaiduApp","--startCode=690000000"};
//		args=new String[]{"--jobName=test"};
//		args=new String[]{"--jobName=SiFaStep1","--uploadDay=2016-03-16"};
		run(args);
	}
}
