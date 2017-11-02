package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

import gs.GsModel;

public abstract class HeadlessSearcher extends Searcher{

	public ExtendedHtmlUnitDriver driver=null;
	public String validateUrl;
	public String curCode;
	public Logger logger;
	public String searchWindowHandle=null;
	private Runtime runtime=Runtime.getRuntime();
	private MSSQLClient dbClient;
	Proxy proxy= new Proxy();
	private int processIdentity=-1;

	public HeadlessSearcher()
	{
		proxy.setHttpProxy("127.0.0.1:19527");  
	}
	
	public void setProcessIdentity(int processIdentity)
	{
		this.processIdentity=processIdentity;
	}
	
	public abstract Object search(String code) throws Exception;

	public int initDriver() throws Exception
	{
		logger.info("Initializing driver...");
		driver=new ExtendedHtmlUnitDriver(true);
//		driver.setProxySettings(proxy);
		logger.info("Driver initialized succeed!");
//		driver.manage().timeouts().pageLoadTimeout(SysConfig.WAIT_IN_SECONDS, TimeUnit.SECONDS);
//		driver.manage().timeouts().implicitlyWait(SysConfig.WAIT_IN_SECONDS, TimeUnit.SECONDS);
		
		for(int i=0;i<SysConfig.MAX_TRY_TIMES;i++)
		{
			if(loadStartPage()==true)
			{
				break;
			}
			else
			{
				logger.info("load start Page failed!");
			}
		}
		logger.info("Loading start URL succeed!");
		searchWindowHandle=driver.getWindowHandle();
		return 0;
//		return initDriver(1);
	}
//	public int initDriver(int t) throws Exception
//	{
//		if(t==SysConfig.MAX_TRY_TIMES)
//		{
//			logger.info("Driver initializing failed!");
//			dbClient.execute("update ProcessStatus set processStatus=1 where processIdentity="+processIdentity);
//			dbClient.commit();
//			
//			logger.close();
//			System.exit(1);
//		}
//		try
//		{
//			logger.info("build driver ...");
//			driver=new ExtendedHtmlUnitDriver(BrowserVersion.INTERNET_EXPLORER_11,true);
//			
//			driver.setProxySettings(proxy);
//			logger.info("build driver succeed!");
//			driver.get(startUrl);
//			logger.info("get search URL succeed!");
//			searchWindowHandle=driver.getWindowHandle();
//			logger.info("Driver initializing succeed!");
//			return 0;
//		}
//		catch (Exception e)
//		{
//			logger.info(SysConfig.getError(e));
//		}
//		return initDriver(t+1);
//	}
	
	public void setLogger(Logger logger)
	{
		this.logger=logger;
	}

	public void setDbClient(MSSQLClient dbClient)
	{
		this.dbClient=dbClient;
	}
	
	public List<WebElement> waitForWebElements(final By eleXpath)
	{
		waitForWebElement(eleXpath);
		return driver.findElements(eleXpath);
	}
	public List<WebElement> waitForWebElements(final WebElement parentEle,final By eleXpath)
	{
		waitForWebElement(parentEle,eleXpath);
		return parentEle.findElements(eleXpath);
	}
	
	public WebElement waitForWebElement(final By eleXpath)
	{
		return waitForWebElement(eleXpath,SysConfig.WAIT_IN_SECONDS);
	}

	public WebElement waitForWebElement(final By eleXpath,int waitInSeconds)
	{
		return (new WebDriverWait(driver,waitInSeconds)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(eleXpath);
        	}        	       	
        });
	}
	
	public WebElement waitForWebElement(final WebElement parentEle,final By eleXpath)
	{
		return waitForWebElement(parentEle,eleXpath,SysConfig.WAIT_IN_SECONDS);
	}
	
	public WebElement waitForWebElement(final WebElement parentEle,final By eleXpath,int waitInSeconds)
	{
		return (new WebDriverWait(driver,waitInSeconds)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return parentEle.findElement(eleXpath);
        	}        	       	
        });
	}

	public String recongnizeValidateCode(String pluginPath,String validateUrl) throws IOException, FailingHttpStatusCodeException, InterruptedException
	{
		String imagePath=SysConfig.getValidateCodeSavePath(curCode);
		driver.downloadImage(validateUrl, imagePath);
		String validateCode=null;
		String cmd="cmd /c "+pluginPath+" "+imagePath;
//		System.out.println(cmd);
		try
		{
			Process process = runtime.exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line=null;
			int i=0;
			while((line=reader.readLine())!=null)
			{
				if((++i)==7)
				{
					validateCode=line.trim();
					reader.close();
					break;
				}
			}
		}
		catch (Exception e)
		{
			logger.info("Recongnize validate code failed.Try again...");
			logger.info(SysConfig.getError(e));
		}
//		new File(imagePath).delete();
		return validateCode;
	}
	
	public int quitDriver() throws IOException
	{
		logger.info("Driver is quitting...");
//		driver.close();
		Set<String> handles=driver.getWindowHandles();
		for(String handle:handles)
		{
			driver.switchTo().window(handle);
			driver.close();
		}
		
		logger.info("Driver quitting succeed!");
		return 0;
	}
}
