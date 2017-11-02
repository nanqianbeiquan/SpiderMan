package sifa;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import tools.MSSQLClient;

public class ParseCourtMap {

	String startPage="http://baike.baidu.com/court";
	WebDriver driver;
	int totoal_cnt=0;
	MSSQLClient dbClient=new MSSQLClient(
			String.format("jdbc:sqlserver://121.42.41.188:1433;DatabaseName=judgment"),
			"likai", //user
			"2$sQNOQTb%", //pwd
			false //autoCommit
			);
	HashMap<String,String> provinceMap=new HashMap<String,String>();
	
	public ParseCourtMap() throws ClassNotFoundException, SQLException
	{
		provinceMap.put("����","����ʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("���ɹ�","���ɹ�������");
		provinceMap.put("�㽭","�㽭ʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("����","����������");
		provinceMap.put("������","������ʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("����","����׳��������");
		provinceMap.put("����","����ʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("ɽ��","ɽ��ʡ");
		provinceMap.put("����ʡ��ɳ��","����ʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("ɽ��","ɽ��ʡ");
		provinceMap.put("�½�","�½�ά���������");
		provinceMap.put("�ຣ","�ຣʡ");
		provinceMap.put("����","����ʡ");
		provinceMap.put("�ӱ�","�ӱ�ʡ");
		provinceMap.put("����","���Ļ���������");
		provinceMap.put("����","������");
		provinceMap.put("���","�����");
		provinceMap.put("�㶫","�㶫ʡ");
		provinceMap.put("�Ϻ�","�Ϻ���");
		provinceMap.put("�Ĵ�","�Ĵ�ʡ");
		provinceMap.put("����","������");
		provinceMap.put("����","����ʡ");
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
	
	public void parseCourts(String provName) throws ClassNotFoundException, SQLException
	{
		while(true)
		{
			List<WebElement> courts = waitForWebElements(By.xpath(".//*[@id='search-box']/div[2]/ul/li"));
			for(WebElement court:courts)
			{
				totoal_cnt++;
//				System.out.println(provName+","+court.getText());
				String courtName=court.getText();
				String province=null;
				if(courtName.equals("�л����񹲺͹��������Ժ"))
				{
					province="�������Ժ";
				}
				else if(courtName.contains("�������"))
				{
					province="�½�ά����������߼�����Ժ����������ŷ�Ժ";
				}
				else
				{
					province=provinceMap.get(provName);
				}
				String level;
				if(courtName.equals("�л����񹲺͹��������Ժ"))
				{
					level="��߷�Ժ";
				}
				else if(courtName.contains("�߼�"))
				{
					level="�߼���Ժ";
				}
				else if(courtName.contains("�м�"))
				{
					level="�м���Ժ";
				}
				else
				{
					level="���㷨Ժ";
				}
				String sql=String.format("insert into lu_court values('%s','%s','%s')", province,level,courtName);
				dbClient.execute(sql);
			}
			int current=Integer.valueOf(waitForWebElement(By.xpath(".//span[@class='current']")).getText());
			int total=Integer.valueOf(waitForWebElement(By.xpath(".//span[@class='total']")).getText());
			if(current==total)
			{
				break;
			}
			WebElement next = waitForWebElement(By.xpath(".//*[@id='search-box']/div[2]/div/span[3]"));
			next.click();
		}
	}
	
	public void run() throws InterruptedException, SQLException, ClassNotFoundException
	{
		driver=new FirefoxDriver();
		driver.manage().window().maximize();
		driver.get(startPage);
		WebElement delElement=waitForWebElement(By.xpath("/html/body/div[2]/div[1]/*[name()='svg']/*[name()='g']/*[name()='g'][4]"));
		((JavascriptExecutor)driver).executeScript("arguments[0].parentNode.removeChild(arguments[0])", delElement);
		int provinceNums=waitForWebElements(By.xpath("/html/body/div[2]/div[1]/*[name()='svg']/*[name()='g']/*[name()='g'][1]/*[name()='g']")).size();
		for(int i=1;i<=provinceNums-4;i++)
		{
			try
			{
				WebElement provinceText = waitForWebElement(By.xpath("/html/body/div[2]/div[1]/*[name()='svg']/*[name()='g']/*[name()='g'][1]/*[name()='g']["+i+"]/*[name()='text']"));
				String provName=provinceText.getText();
				Actions action = new Actions(driver);
				action.click(provinceText).perform();
				WebElement backChina = waitForWebElement(By.xpath(".//div[@id='back-china'][@style='display: block;']"));
				parseCourts(provName);
				backChina.click();
			}
			catch (TimeoutException e)
			{
				driver.get(startPage);
				delElement=waitForWebElement(By.xpath("/html/body/div[2]/div[1]/*[name()='svg']/*[name()='g']/*[name()='g'][4]"));
				((JavascriptExecutor)driver).executeScript("arguments[0].parentNode.removeChild(arguments[0])", delElement);
				i--;
			}
			
		}
		dbClient.commit();
		driver.quit();
	}
	
	public static void main(String[] args) throws InterruptedException, ClassNotFoundException, SQLException
	{
		ParseCourtMap job=new ParseCourtMap();
		job.run();
	}
}
