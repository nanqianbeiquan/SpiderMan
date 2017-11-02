package nacao;

import java.io.IOException;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxProfile;

import tools.HeadlessSearcher;
import tools.Logger;
import tools.SysConfig;

public class BaiduAppSearcherHeadless extends HeadlessSearcher{

	String searchWindowHandle;
	String appDomain="http://boxcomputing.nacao.org.cn/";
	String certificateUrl=null;
	
//	By appWindowXpath=By.xpath(".//*[@tpl='app_normal']//span[text()='进入应用']");
	By appWindowXpath=By.xpath(".//*[@data-click=\"{'fm':'beha'}\"]");
	
	By BAPPIframeXpath=By.xpath(".//iframe[contains(@id,'BAPPIframe')]");
	
	By orgCodeInputXpath=By.xpath(".//*[@id='keyword_1']");
	By validateCodeInputXpath=By.xpath(".//*[@id='validateCode_1']");
	By validateImageXpath=By.xpath(".//*[@id='validateCodeImage_1']");
	By orgCodeSubmitXpath=By.xpath(".//*[@id='con_one_1']/div[1]/ul/li[2]/input[2]");	
	By searchResultXpath=By.xpath(".//div[@class='content']/div[@class]");
	
	WebElement BAPPIframe=null;
	WebElement appButton=null;
	WebElement orgCodeInput=null;
	WebElement validateCodeInput=null;
	WebElement validateImage=null;
	WebElement orgCodeSubmit=null;
	WebElement searchAgain=null;
	WebElement searchResultEle=null;
	WebElement backButton=null;
	FirefoxProfile profile = new FirefoxProfile(); 
	String curCode;
	boolean outFrame=true;
	
	public BaiduAppSearcherHeadless()
	{
		startUrl="http://www.baidu.com/baidu?wd=%D7%E9%D6%AF%BB%FA%B9%B9%B4%FA%C2%EB&tn=monline_4_dg";
//		validateUrl="http://boxcomputing.nacao.org.cn/ValidateCode";
	}
	
	public NACAO search(String orgCode) throws IOException, InterruptedException
	{
		if(outFrame==true)
		{
			switchToAppFrame();
		}
		
		curCode=orgCode;
		NACAO nacao= new NACAO(curCode);
		searchAgain=null;
		try
		{
			orgCodeInput=waitForWebElement(orgCodeInputXpath);
    		validateCodeInput=waitForWebElement(validateCodeInputXpath);
    		validateImage=waitForWebElement(validateImageXpath);
    		validateUrl=appDomain+validateImage.getAttribute("src");
    		
    		orgCodeSubmit=waitForWebElement(orgCodeSubmitXpath);
    		orgCodeInput.sendKeys(curCode);
    		
    		String validateCode="";
    		for(int j=0;j<SysConfig.MAX_TRY_TIMES;j++)
    		{
    			recongnizeValidateCode(SysConfig.BDYY_OCR, validateUrl);
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
    		
    		validateCodeInput.sendKeys(validateCode);
    		orgCodeSubmit.click();
    		
    		searchAgain=waitForWebElement(By.linkText("<< 重新搜索"));
			searchResultEle=waitForWebElement(searchResultXpath);
    		
    		String searchResultType=searchResultEle.getAttribute("class");
    		//查询结果为机构名称
    		if(searchResultType.equals("details_word"))
    		{
    			WebElement orgNameEle=waitForWebElement(By.xpath(".//*[@id='dmzContent']/div[1]/p[2]/span"));
    			String orgName=orgNameEle.getText().trim();
    			nacao.setUpdateStatus(1);
    			nacao.setOrgName(orgName);
    			nacao.setCertificateExists(0);
    		}
    		//查询结果为机构证书,保存证书截图
    		else if(searchResultType.equals("details_picture"))
    		{
    			WebElement certificateImage=driver.findElement(By.xpath(".//*[@id='dmzContent']/div[1]/p/img"));
    			certificateUrl=appDomain+certificateImage.getAttribute("src");
    			logger.info(certificateUrl);
    			driver.downloadImage(certificateUrl, SysConfig.getCertificateSavePath(curCode));
    			
//    			screenShot(SysConfig.getCertificateSavePath(curCode),certificateImage,validateOffsetX,validateOffsetY);
    			nacao.setUpdateStatus(1);
    			nacao.setCertificateExists(1);
    		}
    		else
    		{
    			String errorInfo=searchResultEle.getText().trim();
    			//机构代码不存在
    			if(errorInfo.startsWith("您输入的信息，查询结果为 0 条"))
    			{
    				nacao.setUpdateStatus(0);
    			}
    			//验证码识别失败
    			else
    			{
    				nacao.setUpdateStatus(3);
    			}
    		}
		}
		catch(TimeoutException e)
		{
			e.printStackTrace();
			nacao.setUpdateStatus(3);
			
		}
		if(searchAgain!=null)
		{
			searchAgain.click();
		}
		else
		{
			driver.switchTo().defaultContent();
			backButton.click();
			outFrame=true;
		}
		return nacao;
//		System.out.println("waitForLoadingAppBlock over!");
		
		
		
	}

	public static void main(String[] args) throws Exception
	{
		BaiduAppSearcherHeadless searcher=new BaiduAppSearcherHeadless();
		searcher.setLogger(new Logger("test"));
		searcher.initDriver();
		String[] codeArray={"000000010","802100433","000000019","596247871"};
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
			
		}
		
	}

	public void switchToAppFrame() throws IOException, InterruptedException
	{
		appButton = waitForWebElement(appWindowXpath);
		
		appButton.click();
		
		System.out.println(appButton);
		backButton=waitForWebElement(By.xpath("/html/body/div[3]/div[3]/div[1]/div[3]/div[2]/div[1]/h3/span[1]"));
		BAPPIframe = waitForWebElement(BAPPIframeXpath);
		driver.switchTo().frame(BAPPIframe);
		
		orgCodeInput=waitForWebElement(orgCodeInputXpath);
		validateCodeInput=waitForWebElement(validateCodeInputXpath);
		validateImage=waitForWebElement(validateImageXpath);
		orgCodeSubmit=waitForWebElement(orgCodeSubmitXpath);
		
		driver.switchTo().defaultContent();
		driver.switchTo().frame(BAPPIframe);
		outFrame=false;
	}
	
	@Override
	public boolean loadStartPage() throws IOException {
		
		boolean res=true;
		try
		{
			driver.get(startUrl);
		}
		catch (Exception e)
		{
			res=false;
			logger.info(SysConfig.getError(e));
		}
		return res;
	}

}
