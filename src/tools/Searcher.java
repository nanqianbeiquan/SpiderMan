package tools;

import java.io.IOException;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class Searcher {

	public String startUrl;
	public WebDriver driver=null;
	public Logger logger;
	public String searchWindowHandle=null;
	public boolean IpForbidden=false;
	
	public abstract int initDriver() throws Exception;
	public abstract int quitDriver() throws Exception;
	
	public abstract Object search(String code) throws Exception;
	
	public void setLogger(Logger logger)
	{
		this.logger=logger;
	}
	public abstract boolean loadStartPage() throws IOException;
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
	public void setDbClient(MSSQLClient dbClient) {
		// TODO Auto-generated method stub
		
	}
	public void setProcessIdentity(int processIdentity) {
		// TODO Auto-generated method stub
	}
}
