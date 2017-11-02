package sifa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import tools.JobConfig;
import tools.Logger;
import tools.MSSQLClient;
import tools.SysConfig;

public class SiFaStep1 {
	
	SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
	MSSQLClient dbClient=new MSSQLClient(
			String.format("jdbc:sqlserver://121.42.41.188:1433;DatabaseName=judgment"),
			"likai", //user
			"2$sQNOQTb%", //pwd
			false //autoCommit
			);
	
	WebDriver driver;
	String[] ajlxArr=new String[]{"���°���","���°���","��������","�⳥����","ִ�а���"};
	Logger logger;
	
	String startURL="http://wenshu.court.gov.cn/List/List?sorttype=1";
	String uploadDay;
	int total1=0;
	int total2=0;
	int startIdx=1;
	String curProvince;
	String curURLArgs;
	
//	String currentURL=null;
	public SiFaStep1() throws ClassNotFoundException, SQLException
	{
//		FirefoxProfile profile=new FirefoxProfile(new File(SysConfig.FIREFOX_PROFILE));
//		driver=new FirefoxDriver(profile);
		driver=new HtmlUnitDriver(true);
		driver.manage().timeouts().pageLoadTimeout(15, TimeUnit.SECONDS);
	}

	public List<WebElement> waitForWebElements(final By eleXpath)
	{
		waitForWebElement(eleXpath);
		return driver.findElements(eleXpath);
	}
	
	public WebElement waitForWebElement(final By eleXpath)
	{
		return (new WebDriverWait(driver,5)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(eleXpath);
        	}        	       	
        });
	}
	
	public String getDateArg() throws ParseException, UnsupportedEncodingException
	{
		Calendar calendar = new GregorianCalendar();
        calendar.setTime(sdf.parse(uploadDay));
        calendar.add(Calendar.DATE,1);
        String stopDay = sdf.format(calendar.getTime());     
		String arg1=uploadDay+"%20TO%20"+stopDay;
		String arg2=URLEncoder.encode("�ϴ�����", "UTF-8");
        return String.format("&conditions=searchWord+++%s+%s:%s", arg1,arg2,arg1);
	}
	
	public String getProvinceArg(String province) throws UnsupportedEncodingException
	{
		if(province.equals("�������Ժ"))
		{
			String arg1=URLEncoder.encode("��߷�Ժ", "UTF-8");
			String arg2=URLEncoder.encode("��Ժ�㼶", "UTF-8");
			return "&conditions=searchWord+"+arg1+"+++"+arg2+":"+arg1;
		}
		else
		{
			String arg1=URLEncoder.encode(province, "UTF-8");
			String arg2=URLEncoder.encode("��Ժ����", "UTF-8");
			return String.format("&conditions=searchWord+%s+++%s:%s", arg1,arg2,arg1);
		}
	}
	
	public String getAjlxArg(String ajlx) throws UnsupportedEncodingException
	{
		String arg1=URLEncoder.encode(ajlx, "UTF-8");
		String arg2=URLEncoder.encode("��������", "UTF-8");
		return String.format("&conditions=searchWord+%s+++%s:%s", arg1,arg2,arg1);
	}
	
	public String getCourtNameArg(String courtLevel,String courtName) throws UnsupportedEncodingException
	{
		String arg1=URLEncoder.encode(courtName, "UTF-8");
		String arg2=URLEncoder.encode(courtLevel, "UTF-8");
		
		if(courtLevel.equals("�߼���Ժ"))
		{
			String arg3=URLEncoder.encode("�߼���Ժ", "UTF-8");
			String arg4=URLEncoder.encode("��Ժ�㼶", "UTF-8");
			return String.format("&conditions=searchWord+%s+++%s:%s&conditions=searchWord+%s+++%s:%s", arg1,arg2,arg1,arg3,arg4,arg3);
		}
		else
		{
			return String.format("&conditions=searchWord+%s+++%s:%s", arg1,arg2,arg1);
		}
		
		
		
	}
	
	public void loadAll() throws ClassNotFoundException, SQLException, ParseException, IOException, InterruptedException
	{
		
		logger.info("--------------------------��ʼ����------------------------------");
		logger.info("�ϴ����ڣ�"+uploadDay);
		curURLArgs=getDateArg();
		String URL=startURL+curURLArgs;
		logger.info(URL);
		getPage(URL);
		int courtNums=getCourtNums();
		total1=courtNums;
		logger.info("����������"+courtNums);
		if(courtNums>200)
		{
			logger.info("������������200����ʡ����ץȡ");
			ArrayList<String> provList=new ArrayList<String>();
			ResultSet res = dbClient.execute("select distinct province from lu_court_zhongji");
			while(res.next())
			{
				provList.add(res.getString("province"));
			}
			res.close();
			for(String province:provList)
			{
				curProvince=province;
				loadProvince(province);
			}
		}
		else
		{
			total2+=courtNums;
			loadCurrentPage();
		}
	}

	public void loadProvince(String province) throws ParseException, ClassNotFoundException, SQLException, IOException, InterruptedException
	{
		logger.info("--------------------------"+province+"------------------------------");
//		logger.info("ץȡʡ�ݣ�"+province);
		curURLArgs=getDateArg()+getProvinceArg(province);
		String URL=startURL+curURLArgs;
		logger.info(URL);
		getPage(URL);
		int courtNums=getCourtNums();
		if(courtNums>200)
		{
			logger.info("������������200���ְ������ͽ���ץȡ");
			for(String ajlx:ajlxArr)
			{
				loadProvinceAjlx(province,ajlx);
			}
		}
		else
		{
			logger.info(province+" : "+courtNums);
			total2+=courtNums;
			loadCurrentPage();
		}
	}
	
	public void loadProvinceAjlx(String province,String ajlx) throws ParseException, SQLException, ClassNotFoundException, IOException, InterruptedException
	{
		logger.info("--------------------------"+province+"&"+ajlx+"------------------------------");
//		logger.info("�������ͣ�"+ajlx);
		curURLArgs=getDateArg()+getProvinceArg(province)+getAjlxArg(ajlx);
		String URL=startURL+curURLArgs;
		logger.info(URL);
		getPage(URL);
		int courtNums=getCourtNums();
//		logger.info("����������"+courtNums);
		if(courtNums>200)
		{
			logger.info("������������200���ַ�Ժ����ץȡ");
			ResultSet res = dbClient.execute(String.format("select court_level,court_name from lu_court_zhongji where province='%s'",province));
			
			List<String> courtLevelList=new ArrayList<String>();
			List<String> courtNameList=new ArrayList<String>();
			while(res.next())
			{
				courtLevelList.add(res.getString("court_level"));
				courtNameList.add(res.getString("court_name"));
			}
			res.close();
			
			for(int i=0;i<courtLevelList.size();i++)
			{
				String courtLevel=courtLevelList.get(i);
				String courtName=courtNameList.get(i);
				loadProvinceAjlxCourt(province,ajlx,courtLevel,courtName);
			}
		}
		else
		{
			logger.info(province+"&"+ajlx+" : "+courtNums);
			total2+=courtNums;
			loadCurrentPage();
		}
	}
	
	public void loadProvinceAjlxCourt(String province,String ajlx,String courtLevel,String courtName) throws ParseException, IOException, InterruptedException, ClassNotFoundException, SQLException
	{
		logger.info("--------------------------"+province+"&"+ajlx+"&"+courtName+"------------------------------");
		curURLArgs=getDateArg()+getProvinceArg(province)+getAjlxArg(ajlx)+getCourtNameArg(courtLevel, courtName);
		String URL=startURL+curURLArgs;
		logger.info(URL);
		getPage(URL);
		int courtNums=getCourtNums();
		logger.info(province+"&"+ajlx+"&"+courtName+" : "+courtNums);
		total2+=courtNums;
		loadCurrentPage();
	}
	
	public void setItemNumsTo20()
	{
		WebElement input = waitForWebElement(By.xpath("/html/body/div[1]/div[2]/div[1]/div[2]/div[2]/div[5]/div/div[2]/div/table/tbody/tr/td[2]/input"));
		input.click();
		WebElement li = waitForWebElement(By.xpath("/html/body/div[1]/div[2]/div[1]/div[2]/div[2]/div[5]/div/div[2]/div/div[1]/ul/li[4]"));
		li.click();
	}
	
	public int getCourtNums() throws IOException, InterruptedException
	{
		int waitCnt=0;
		while(true)
		{
			waitCnt++;
			String resultText=waitForWebElement(By.xpath("//div[@id='resultList']")).getText().trim();
//			logger.info(resultText);
			if(!resultText.equals("���ڼ��أ����Ժ�...") && !resultText.equals(""))
			{
				break;
			}
			logger.info("���ڼ��أ����Ժ�...");
			if(waitCnt%30==0)
			{
				refreshPage();
			}
			else
			{
				TimeUnit.MILLISECONDS.sleep(1000);
			}
		}
		
		WebElement dataCnt = driver.findElement(By.xpath("//*[@id='span_datacount']"));
		return Integer.valueOf(dataCnt.getText());
	}
	
	public WebElement waitForWebElement(final WebElement parentEle,final By eleXpath)
	{
		return (new WebDriverWait(driver,5)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return parentEle.findElement(eleXpath);
        	}        	       	
        });
	}

	public void getPage(String URL) throws IOException
	{
		while (true)
		{
			try
			{
				logger.info("����ҳ��...");
				driver.get(URL);;
				break;
			}
			catch (Exception e)
			{
				logger.info(SysConfig.getError(e));
				logger.info("����ҳ��ʧ�ܣ�����");
				continue;
			}
		}
	}
	
	public void refreshPage() throws IOException, InterruptedException
	{
		String curPage = driver.getCurrentUrl();
//		int driverStatus=1;
		while (true)
		{
			try
			{
				driver.close();
				driver=new HtmlUnitDriver(true);
//				if(driverStatus==1)
//				{
//					driver.close();
//					driverStatus=-1;
//				}
//				if(driverStatus==-1)
//				{
//					driver=new HtmlUnitDriver(true);
//					driverStatus=1;
//				}
				driver.get(curPage);
//				driver.navigate().refresh();
				logger.info("�����������ˢ��ҳ��...");
				setItemNumsTo20();
				stepIntoStartIdx();
				break;
			}
			catch (Exception e)
			{
				logger.info(SysConfig.getError(e));
				logger.info("ˢ��ҳ��ʧ�ܣ�����");
			}
		}
		
	}
	
	public void stepIntoStartIdx() throws IOException
	{
		int i=0;
		while(Integer.valueOf(waitForWebElement(By.xpath("/html/body/div[1]/div[2]/div[1]/div[2]/div[2]/div[5]/div/span[@class='current']")).getText())<startIdx)
		{
			i++;
			logger.info("��ת����"+i+"ҳ");
			waitForWebElement(By.xpath("/html/body/div[1]/div[2]/div[1]/div[2]/div[2]/div[5]/div/*[@class][last()]")).click();
		}
	}
	
	public void loadCurrentPage() throws IOException, InterruptedException, ClassNotFoundException, SQLException
	{
		try
		{
			setItemNumsTo20();
			while(getCourtNums()>0)
			{
				logger.info("��"+startIdx+"ҳ");
				List<WebElement> dataItemList = waitForWebElements(By.xpath(".//*[@id='resultList']/div"));
//				logger.info("������");
				for(int i=1;i<=dataItemList.size();i++)
				{
//					logger.info("��λfilePage...");
					String filePage=waitForWebElement(By.xpath(".//*[@id='resultList']/div["+i+"]/table/tbody/tr[1]//a[@href]")).getAttribute("href");
					String fileName=filePage.substring(filePage.indexOf("DocID=")+6);
//					logger.info("��λtitle...");
					String title=waitForWebElement(By.xpath(".//*[@id='resultList']/div["+i+"]/table/tbody/tr[1]")).getText();
//					logger.info("��λotherInfo...");
					String otherInfo=waitForWebElement(By.xpath(".//*[@id='resultList']/div["+i+"]/table/tbody/tr[2]")).getText();
					int idx1=otherInfo.indexOf(" ");
					int idx2=otherInfo.lastIndexOf(" ");
					String courtName=otherInfo.substring(0, idx1-1);
					String code=otherInfo.substring(idx1, idx2).trim();
					String date=otherInfo.substring(idx2+1).trim();
//					logger.info("�������ݿ�...");
					ResultSet res = dbClient.execute(String.format("select updateStatus from DocumentPage where filePage='%s'",filePage));
					if(!res.next())
					{
						dbClient.execute(String.format("insert into DocumentPage(filePage,title,courtName,code,uploadDate,updatestatus,lastUpdateTime,fileNameLocal) "
								+ "values('%s','%s','%s','%s','%s',0,getDate(),'%s')",filePage,title,courtName,code,date,fileName));
					}
//					logger.info("����ɹ���");
				}
//				logger.info("��λ��һҳ");
				WebElement nextPage = waitForWebElement(By.xpath("/html/body/div[1]/div[2]/div[1]/div[2]/div[2]/div[5]/div/*[@class][last()]"));
				if(nextPage.getTagName().equals("a"))
				{
//					logger.info("�����һҳ");
					nextPage.click();
					startIdx++;
				}
				else
				{
//					logger.info("��������һҳ");
					break;
				}
			}
			dbClient.commit();
			startIdx=1;
		}
		catch (Exception e)
		{
			logger.info(SysConfig.getError(e));
			logger.info("�����������½��н���");
			refreshPage();
			loadCurrentPage();
		}
	}

	public void run(JobConfig jobConf) throws ClassNotFoundException, SQLException, ParseException, IOException, InterruptedException
	{
		Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE,-1);
        uploadDay = sdf.format(calendar.getTime());     
        
		if(jobConf.hasProperty("uploadDay"))
		{
			uploadDay=jobConf.getString("uploadDay");
		}
		
		logger=new Logger("SifaStep1_"+uploadDay);
		loadAll();
		logger.info("��������:"+total1+","+total2);
		dbClient.commit();
		driver.quit();
		logger.info("������ɣ�");
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, ParseException
	{
		
	}
}
