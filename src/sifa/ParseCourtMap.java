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
		provinceMap.put("辽宁","辽宁省");
		provinceMap.put("湖南","湖南省");
		provinceMap.put("内蒙古","内蒙古自治区");
		provinceMap.put("浙江","浙江省");
		provinceMap.put("安徽","安徽省");
		provinceMap.put("贵州","贵州省");
		provinceMap.put("西藏","西藏自治区");
		provinceMap.put("黑龙江","黑龙江省");
		provinceMap.put("陕西","陕西省");
		provinceMap.put("广西","广西壮族自治区");
		provinceMap.put("湖北","湖北省");
		provinceMap.put("福建","福建省");
		provinceMap.put("山西","山西省");
		provinceMap.put("海南省三沙市","海南省");
		provinceMap.put("江西","江西省");
		provinceMap.put("海南","海南省");
		provinceMap.put("江苏","江苏省");
		provinceMap.put("云南","云南省");
		provinceMap.put("河南","河南省");
		provinceMap.put("山东","山东省");
		provinceMap.put("新疆","新疆维吾尔自治区");
		provinceMap.put("青海","青海省");
		provinceMap.put("吉林","吉林省");
		provinceMap.put("河北","河北省");
		provinceMap.put("宁夏","宁夏回族自治区");
		provinceMap.put("北京","北京市");
		provinceMap.put("天津","天津市");
		provinceMap.put("广东","广东省");
		provinceMap.put("上海","上海市");
		provinceMap.put("四川","四川省");
		provinceMap.put("重庆","重庆市");
		provinceMap.put("甘肃","甘肃省");
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
				if(courtName.equals("中华人民共和国最高人民法院"))
				{
					province="最高人民法院";
				}
				else if(courtName.contains("建设兵团"))
				{
					province="新疆维吾尔自治区高级人民法院生产建设兵团分院";
				}
				else
				{
					province=provinceMap.get(provName);
				}
				String level;
				if(courtName.equals("中华人民共和国最高人民法院"))
				{
					level="最高法院";
				}
				else if(courtName.contains("高级"))
				{
					level="高级法院";
				}
				else if(courtName.contains("中级"))
				{
					level="中级法院";
				}
				else
				{
					level="基层法院";
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
