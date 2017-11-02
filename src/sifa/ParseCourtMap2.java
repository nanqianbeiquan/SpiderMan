package sifa;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import tools.MSSQLClient;

public class ParseCourtMap2 {
	
	WebDriver driver;
	String startPage="http://wenshu.court.gov.cn/list/list/?sorttype=1";
	
	int curI=1;
	int curJ=1;
	MSSQLClient dbClient=new MSSQLClient(
			String.format("jdbc:sqlserver://121.42.41.188:1433;DatabaseName=judgment"),
			"likai", //user
			"2$sQNOQTb%", //pwd
			false //autoCommit
			);
	
	public ParseCourtMap2() throws ClassNotFoundException, SQLException
	{
		
		driver=new FirefoxDriver();
		driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
		driver.manage().window().maximize();
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
	
	public void getPage()
	{
		try {
			driver.get(startPage);
			waitForWebElement(By.xpath(".//div[@key='法院地域']/div[3]")).click();
		} catch (TimeoutException e) {
			// TODO: handle exception
			System.out.println("Get start page timeout!Try again!");
			getPage();
		}
	}
	
	public void run() throws InterruptedException, SQLException
	{
		getPage();
		try
		{
			for(int i=1;i<=waitForWebElements(By.xpath(".//div[@key='法院地域']/div[2]/ul/li")).size();i++)
			{
				if(i<curI)
				{
					continue;
				}
				curI=i;
				WebElement province=waitForWebElement(By.xpath(String.format(".//div[@key='法院地域']/div[2]/ul/li[%d]", i)));
				String provName=province.getText();
				provName=provName.substring(0, provName.indexOf("("));
				System.out.println("法院地域："+provName);
				if(!province.getAttribute("class").equals("jstree-node  jstree-leaf"))
				{
					waitForWebElement(By.xpath(String.format(".//div[@key='法院地域']/div[2]/ul/li[%d]/i", i))).click();
//					int try1=0;
//					while(waitForWebElement(By.xpath(String.format(".//div[@key='法院地域']/div[2]/ul/li[%d]/ul", i))).getText().contains("此节点加载中..."))
//					{
//						try1++;
//						if((try1)%5==0)
//						{
//							throw new TimeoutException();
//						}
//						System.out.println("此节点加载中...");
//						TimeUnit.SECONDS.sleep(2);
//					}
//					TimeUnit.SECONDS.sleep(1);
//					System.out.println(waitForWebElements(By.xpath(String.format(".//div[@key='法院地域']/div[2]/ul/li[%d]/ul/li", i))).size());
					curJ=1;
					for(int j=1;j<=waitForWebElements(By.xpath(String.format(".//div[@key='法院地域']/div[2]/ul/li[%d]/ul/li[not(contains(@id,'NULL'))]", i))).size();j++)
//					for(int j=1;j<=waitForWebElements(By.xpath(String.format(".//div[@key='法院地域']/div[2]/ul/li[%d]/ul/li", i))).size();j++)
					{
						if(j<curJ)
						{
							continue;
						}
						curJ=j;
						WebElement court = waitForWebElement(By.xpath(String.format(".//div[@key='法院地域']/div[2]/ul/li[%d]/ul/li[%d]", i,j)));
						String courtName=court.getText();
						courtName=courtName.substring(0, courtName.indexOf("("));
//						System.out.println("中级法院："+courtName);
						String sql=String.format("insert into lu_court_zhongji values('%s','中级法院','%s')",provName,courtName);
//						System.out.println(sql);
						dbClient.execute(sql);
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			run();
		}
		dbClient.commit();
	}
	
	public static void main(String[] args) throws InterruptedException, ClassNotFoundException, SQLException
	{
		ParseCourtMap2 job =new ParseCourtMap2();
		job.run();
	}
}
