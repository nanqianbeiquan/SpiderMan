package nacao;

import java.io.IOException;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import tools.Logger;
import tools.SearcherChrome;
import tools.SearcherFirefox;
import tools.SearcherPhantomJS;
import tools.SysConfig;

public class BaiduAppSearcher extends SearcherFirefox{

	String searchWindowHandle;

//	By appWindowXpath=By.xpath(".//img[@class='op-app_normal-icon-img']");
	By appWindowXpath=By.xpath(".//div[@class='op-app_normal-box']/div[1]/div[2]/div/span");
	By APPIframeXpath=By.xpath(".//iframe[contains(@id,'BAPPIframe')]");
	
	By orgCodeInputXpath=By.xpath(".//input[@id='keyword_1']");
	By validateCodeInputXpath=By.xpath(".//input[@id='validateCode_1']");
	By validateImageXpath=By.xpath("/html/body/div/div[3]/div[1]/div[2]/div[1]/div[1]/ul/li[2]/a/img");
	By orgCodeSubmitXpath=By.xpath("/html/body/div/div[3]/div[1]/div[2]/div[1]/div[1]/ul/li[2]/input[2]");	
	By searchResultXpath=By.xpath("html/body/div/div/div[@class='details_word' or @class='details_picture' or @class='div_head' or @class='error']");
	WebElement APPIframe=null;
	WebElement appButton=null;
	WebElement orgCodeInput=null;
	WebElement validateCodeInput=null;
	WebElement validateImage=null;
	WebElement orgCodeSubmit=null;
	WebElement searchAgain=null;
	WebElement searchResultEle=null;
	WebElement backButton=null;
	WebElement researchButton=null;
	String curCode;
	
	boolean needLoadStartPage=false;
	boolean needClickBack=false;
	boolean needEnterAppIframe=false;
	boolean firstLoad=true;
	
	public BaiduAppSearcher()
	{
		startUrl="https://www.baidu.com/baidu?wd=%D7%E9%D6%AF%BB%FA%B9%B9%B4%FA%C2%EB&tn=monline_4_dg";
	}
	
	public NACAO search(String orgCode) throws IOException
	{
		curCode=orgCode;
		NACAO nacao= new NACAO(curCode);
		if(needLoadStartPage)
		{
			if(loadStartPage()==false)
			{
				nacao.setUpdateStatus(3);
				needLoadStartPage=true;
				return nacao;
			}
		}
		else if(needClickBack)
		{
			if(clickBack()==true)
			{
				needEnterAppIframe=true;
			}
			else
			{
				nacao.setUpdateStatus(3);
				needLoadStartPage=true;
				return nacao;
			}
		}
		logger.info("needEnterAppIframe:"+needEnterAppIframe);
		if(needEnterAppIframe==true)
		{
			if(enterAppIframe()==false)
			{
				if(IpForbidden==true)
				{
					nacao.setUpdateStatus(4);
				}
				else
				{
					nacao.setUpdateStatus(3);
					needLoadStartPage=true;
				}
				return nacao;
			}
		}
		needLoadStartPage=false;
		needClickBack=false;
		needEnterAppIframe=false;
		try
		{
			logger.info("加载orgCodeInput...");
			orgCodeInput=driver.findElement(orgCodeInputXpath);
			logger.info("加载validateCodeInput...");
			validateCodeInput=driver.findElement(validateCodeInputXpath);
			logger.info("加载validateImage...");
			validateImage=driver.findElement(validateImageXpath);
			logger.info("加载orgCodeSubmit...");
			orgCodeSubmit=driver.findElement(orgCodeSubmitXpath);
			logger.info("输入查询代码...");
//    		orgCodeInput.clear();
//    		orgCodeInput.sendKeys(curCode);
    		((JavascriptExecutor)driver).executeScript("arguments[0].value='"+curCode+"'", orgCodeInput);
    		String validateCode="";
    		for(int j=0;j<SysConfig.MAX_TRY_TIMES;j++)
    		{
    			logger.info("验证码截图...");
    			screenShot(SysConfig.getValidateCodeSavePath(curCode),validateImage,validateOffsetX,validateOffsetY);
    			logger.info("验证码识别...");
    			validateCode=recongnizeValidateCode(SysConfig.BDYY_OCR,SysConfig.getValidateCodeSavePath(curCode));
    			if("".equals(validateCode))
    			{
    				validateImage.click();
    			}
    			else 
    			{
    				break;
    			}
    		}
    		logger.info("输入验证码...");
//    		validateCodeInput.clear();
//    		validateCodeInput.sendKeys(validateCode);
    		((JavascriptExecutor)driver).executeScript("arguments[0].value='"+validateCode+"'", validateCodeInput);
    		logger.info("提交查询...");
    		try
    		{
    			((JavascriptExecutor)driver).executeScript("_submit(1)");
    		}
    		catch (WebDriverException e)
    		{
    			logger.info("提交查询失败");
    			logger.info(SysConfig.getError(e));
    			needClickBack=true;
    			nacao.setUpdateStatus(3);
    			return nacao;
    		}
    		logger.info("提交查询成功");
//    		orgCodeSubmit.click();
		}
		catch (Exception e)
		{
			logger.info(SysConfig.getError(e));
			needClickBack=true;
			nacao.setUpdateStatus(3);
			return nacao;
		}
		
		try
		{
    		logger.info("等待查询结果...");
			searchResultEle=driver.findElement(searchResultXpath);
    		
    		String searchResultType=searchResultEle.getAttribute("class");
    		//查询结果为机构名称
    		if(searchResultType.equals("details_word"))
    		{
    			logger.info("查询结果：机构名称");
    			WebElement orgNameEle=driver.findElement(By.xpath(".//*[@id='dmzContent']/div[1]/p[2]/span"));
    			String orgName=orgNameEle.getText().trim();
    			nacao.setUpdateStatus(1);
    			nacao.setOrgName(orgName);
    			nacao.setCertificateExists(0);
    		}
    		//查询结果为机构证书,保存证书截图
    		else if(searchResultType.equals("details_picture"))
    		{
    			logger.info("查询结果：机构证书");
    			//拉动屏幕到最顶部
    			((JavascriptExecutor)driver).executeScript("var q=document.documentElement.scrollTop=0");
    			WebElement certificateImage=driver.findElement(By.xpath(".//*[@id='dmzContent']/div[1]/p/img"));
    			screenShot(SysConfig.getCertificateSavePath(curCode),certificateImage,validateOffsetX,validateOffsetY);
    			nacao.setUpdateStatus(1);
    			nacao.setCertificateExists(1);
    		}
    		else if(searchResultType.equals("div_head"))
    		{
    			logger.info("查询结果：统一社会信用代码");
    			nacao.setUpdateStatus(1);
    			nacao.setCertificateExists(0);
    			WebElement registeredCodeEle=driver.findElement(By.xpath(".//*[@id='dmzContent']/div[1]/h3"));
    			WebElement orgNameEle=driver.findElement(By.xpath(".//*[@id='dmzContent']/div[2]/ul/li[2]"));
    			WebElement address=driver.findElement(By.xpath(".//*[@id='dmzContent']/div[2]/ul/li[4]"));
    			
    			nacao.setRegisteredAddress(address.getText().trim());
    			nacao.setOrgName(orgNameEle.getText().trim());
    			nacao.setRegisteredCode(registeredCodeEle.getText().replace("统一社会信用代码：", "").trim());
    		}
    		else if(searchResultType.equals("error"))
    		{
    			String errorInfo=searchResultEle.getText().trim();
    			//机构代码不存在
    			if(errorInfo.startsWith("您输入的信息，查询结果为 0 条"))
    			{
    				logger.info("查询结果为 0 条");
    				nacao.setUpdateStatus(0);
    			}
    			//验证码识别失败
    			else if(errorInfo.startsWith("验证码信息验证错误"))
    			{
    				logger.info("验证码信息验证错误");
    				nacao.setUpdateStatus(3);
    			}
    		}
    		else
    		{
    			throw new Exception("未知错误，请人工处理");
    		}
		}
		catch(Exception e)
		{
			logger.info(SysConfig.getError(e));
			logger.info("查询失败[2]");
			nacao.setUpdateStatus(3);
		}
		try
		{
			researchButton=driver.findElement(By.xpath(".//a[@href='/index.jsp']"));
			researchButton.click();
		}
		catch (Exception e)
		{
			logger.info(SysConfig.getError(e));
			needClickBack=true;
		}
		return nacao;	
	}
	
	public boolean clickBack() throws IOException
	{
		logger.info("点击返回,退出应用Iframe...");
		boolean res=true;
		try
		{
			driver.switchTo().defaultContent();
			driver.findElement(By.xpath(".//span[@class='op-app_normal-return OP_LOG_BTN']/span")).click();
		}
		catch(Exception e)
		{
			res=false;
		}
		logger.info("点击返回结果:"+res);
		return res;
	}
	
	public boolean enterAppIframe() throws IOException
	{
		logger.info("进入百度应用Iframe...");
		boolean res=true;
		try
		{
			driver.switchTo().defaultContent();
			logger.info("定位AppButton...");
			appButton = driver.findElement(appWindowXpath);
			logger.info("点击AppButton...");
//			appButton.click();
			((JavascriptExecutor)driver).executeScript("arguments[0].click();", appButton);
			logger.info("定位AppIframe...");
			APPIframe = driver.findElement(APPIframeXpath);
			logger.info("进入AppIframe...");
			driver.switchTo().frame(APPIframe);
			try
			{
				logger.info("定位orgCodeInput...");
				orgCodeInput=driver.findElement(orgCodeInputXpath);
				logger.info("定位validateImage...");
				validateImage=driver.findElement(validateImageXpath);
				if(validateImage.getAttribute("src").equals(""))
				{
					logger.info("刷新validateImage...");
					validateImage.click();
				}
			}
			catch (NoSuchElementException e)
			{
				try
				{
					WebElement errorEle = driver.findElement(By.xpath("/html/body/div/div[2]/div[1]"));
					if(errorEle.getText().contains("您所在的IP超出系统最大限定访问数"))
					{
						IpForbidden=true;
						logger.info("您所在的IP超出系统最大限定访问数");
					}
				}
				catch (NoSuchElementException e2)
				{
					throw new NoSuchElementException(validateImageXpath.toString());
				}
			}
			if (IpForbidden==true)
			{
				res=false;
			}
			else
			{
				logger.info("进入顶层...");
				driver.switchTo().defaultContent();
				logger.info("获取AppIframe坐标...");
				Point iframeLocation = APPIframe.getLocation();
				validateOffsetX=iframeLocation.getX();
				validateOffsetY=iframeLocation.getY();
				logger.info("进入APPIframe...");
				driver.switchTo().frame(APPIframe);
			}
			
		}
		catch (Exception e)
		{	
			logger.info(SysConfig.getError(e));
			res=false;
		}
		logger.info("进入百度应用Iframe结果："+res);
		if(res==false)
		{
			needEnterAppIframe=true;
			
		}
		return res;
	}
	
	@Override
	public boolean loadStartPage() throws IOException {
		logger.info("加载开始页...");		
		boolean res=true;
		if(IpForbidden==true)
		{
			return false;
		}
		try
		{
			if(firstLoad)
			{
				driver.get(startUrl);
				firstLoad=false;
			}
			else
			{
				logger.info("F5刷新页面...");
				driver.navigate().refresh();
//				Actions action = new Actions(driver);
			}
		}
		catch (TimeoutException e)
		{
			// 
		}
		res=enterAppIframe();
		logger.info("加载开始页结果:"+res);
		return res;
	}

	public static void main(String[] args) throws Exception
	{
		BaiduAppSearcher searcher=new BaiduAppSearcher();
		searcher.setLogger(new Logger("test"));
		
		searcher.initDriver();
		String[] codeArray={"691149102","000000010","802100433","000000019","596247871"};
		for(String code:codeArray)
		{
			for(int i=0;i<5;i++)
			{
				NACAO nacao = searcher.search(code);
				System.out.println(nacao);
				if(nacao.updateStatus==1 || nacao.updateStatus==0)
				{
					break;
				}
			}
//			break;
		}
	}
	
}
