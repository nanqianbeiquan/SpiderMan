package nacao;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import tools.HeadlessSearcher;
import tools.Logger;
import tools.SearcherFirefox;
import tools.SysConfig;

public class NacaoOrgSearcher_2 extends SearcherFirefox
{
	By validateIframeXpath=By.xpath(".//*[@id='ym-ml']/div/div/div/iframe");
	WebElement orgCodeInput=null;
	By orgCodeInputXpath=By.xpath(".//input[@name='tit0']");
		
	WebElement orgCodeSubmit=null;
	By orgCodeSubmitXpath=By.xpath(".//a[@onclick='submitForm(0)']/input[@type='button']");
	By loadResultTdXpath=By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[@align][not(@valign)]");

	WebElement certificateIframe=null;
	By certificateIframeXpath=By.xpath(".//*[@id='highslide-wrapper-0']/div[1]/div/div/div[2]/iframe");
	
	Runtime runtime=Runtime.getRuntime();
	
	WebElement validateIframe=null;
	
	WebElement validateInput=null;
	By validateInputXpath=By.xpath(".//*[@id='validateCodeId']");
	WebElement validateSubmit=null;
	By validateSubmitXpath=By.xpath("html/body/div[1]/table/tbody/tr[3]/td[3]/input");

	By ymWindowXpath=By.xpath("/html/body/div[1]/div[2][contains(@style,'display: none')]");
	
	SimpleDateFormat sdf1=new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat sdf2=new SimpleDateFormat("yyyy年M月d日");
	private String curCode;
	
	public NacaoOrgSearcher_2()
	{
		startUrl="https://s.nacao.org.cn";
	}
	
	public boolean loadStartPage() throws IOException
	{
		boolean res=true;
		try
		{
			driver.get(startUrl);
			orgCodeInput=waitForWebElement(orgCodeInputXpath);
			orgCodeSubmit=waitForWebElement(orgCodeSubmitXpath);
		}
		catch (TimeoutException e)
		{
			driver.getWindowHandle();
			try
			{
				orgCodeInput=waitForWebElement(orgCodeInputXpath);
				orgCodeSubmit=waitForWebElement(orgCodeSubmitXpath);
			}
			catch (TimeoutException e2)
			{
				res=false;
			}
		}
		catch (Exception e)
		{
			logger.info(SysConfig.getError(e));
		}
		return res;
	}
	
	public int clickSubmitButton() throws IOException
	{
		int res=0;
		try
		{
			orgCodeSubmit.click();
//			((JavascriptExecutor)driver).executeScript("submitForm(0)");
			if(switchToDetailPage()==true)
			{
				validateIframe=waitForWebElement(validateIframeXpath);
			}
			else
			{
				logger.info("Switch To Detail Page failed!");
				res=3;
			}
		}
		catch (TimeoutException e)
		{
			if(switchToDetailPage()==false)
			{
				res=3;
			}
			else
			{
				try
				{
					validateIframe=waitForWebElement(validateIframeXpath);
				}
				catch (TimeoutException e2)
				{
					res=3;
					try
					{
						WebElement resEle = waitForWebElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td"),2);
						String resText=resEle.getText().trim();
						if(resText.startsWith("您所在的IP暂时不能进行检索"))
						{
							res=4;
						}
					}
					catch (TimeoutException e3)
					{
						//
					}
				}
			}
		}
		return res;
	}
	
	public NACAO search(String orgCode) throws Exception
	{
		curCode=orgCode;
		NACAO nacao= new NACAO(curCode);
		try
		{
			//提交查询请求
			
			orgCodeInput.clear();
			orgCodeInput.sendKeys(curCode);
			int clickRes=clickSubmitButton();
			logger.info("clickRes:"+clickRes);
			if(clickRes!=0)
			{
				if(driver.getWindowHandles().size()>1 && !driver.getCurrentUrl().contains("specialResult.html"))
				{
					switchToDetailPage();
					validateIframe=waitForWebElement(validateIframeXpath);
				}
				if(driver.getCurrentUrl().contains("specialResult.html"))
				{
					logger.info("刷新详情页");
					driver.navigate().refresh();
					validateIframe=waitForWebElement(validateIframeXpath);
				}
				else
				{
					nacao.setUpdateStatus(clickRes);
					switchToSearchPage();
					return nacao;
				}
			}
			
			validateOffsetX=validateIframe.getLocation().getX();
			validateOffsetY=validateIframe.getLocation().getY();

			driver.switchTo().frame(validateIframe);
			validateInput=waitForWebElement(validateInputXpath);

			for(int i=0;i<SysConfig.MAX_TRY_TIMES;i++)
			{
				//获取验证码
				validateImage=waitForWebElement(By.xpath(".//img[@id='validateImage']"));
				screenShot(SysConfig.getValidateCodeSavePath(curCode),validateImage,validateOffsetX,validateOffsetY);
				String validateCode=recongnizeValidateCode(SysConfig.ZZJGDM_OCR,SysConfig.getValidateCodeSavePath(curCode));
				logger.info("validateCode:"+validateCode);
				try
				{
					((JavascriptExecutor)driver).executeScript("arguments[0].value='"+validateCode+"'", validateInput);
					((JavascriptExecutor)driver).executeScript("validateCode()");
				}
				catch (ElementNotVisibleException e)
				{
					driver.switchTo().defaultContent();
					break;
				}
				try
				{
					driver.switchTo().defaultContent();
					if(waitForYmWindow())
					{
						break;
					}
				}
				catch (TimeoutException e)
				{
					logger.info("验证码错误，重新输入!");
					driver.switchTo().frame(validateIframe);
					try
					{
						validateInput.clear();
					}
					catch (ElementNotVisibleException e2)
					{
						driver.switchTo().defaultContent();
						break;
					}
				}
			}
			WebElement loadResult=waitForWebElement(loadResultTdXpath);
			if("检索结果0条".equals(loadResult.getText().trim()))
			{
				nacao.setUpdateStatus(0);
			}
			else
			{
				nacao.setUpdateStatus(1);
				nacao.setCertificateExists(0);
				//机构名称
				WebElement nameEle = loadResult.findElement(By.xpath("following-sibling::td[1]"));
				nacao.setOrgName(nameEle.getText().trim());
				//编号
				WebElement registerNbrEle = loadResult.findElement(By.xpath("following-sibling::td[2]"));
				nacao.setRegisteredCode(registerNbrEle.getText().trim());
				//证书
				WebElement certificateEle = loadResult.findElement(By.xpath("following-sibling::td[3]"));
				if(!"*".equals(certificateEle.getText()))
				{
					nacao.setCertificateExists(1);
					String imageSrc = certificateEle.findElement(By.xpath("a")).getAttribute("href");
					
					imageSrc=URLDecoder.decode(imageSrc,"utf8");
					imageSrc=URLDecoder.decode(imageSrc,"utf8");
					URL url=new URL(imageSrc);
					
					String startDate=null,stopDate=null;
					String query=url.getQuery();
					
					String[] parameterArr=query.split("&");
					for(String parameter:parameterArr)
					{
						String[] info=parameter.split("=",-1);
						if("jgdz".equals(info[0]) && !"null".equals(info[1]))
						{
							nacao.setRegisteredAddress(info[1].trim());
						}
						else if("bzjgmc".equals(info[0]) && !"null".equals(info[1]))
						{
							nacao.setIssuingAuthority(info[1].trim());
						}
						else if("jglx".equals(info[0]) && !"null".equals(info[1]))
						{
							nacao.setOrgType(info[1].trim());
						}
						else if("bzrq".equals(info[0]) && !"null".equals(info[1]))
						{
							try
							{
								startDate=sdf2.format(sdf1.parse(info[1].trim()));
							}
							catch(ParseException e)
							{
//								logger.info(SysConfig.getError(e));
							}
						}
						else if("zfrq".equals(info[0]) && !"null".equals(info[1]))
						{
							try
							{
								stopDate=sdf2.format(sdf1.parse(info[1].trim()));
							}
							catch(ParseException e)
							{
//								logger.info(SysConfig.getError(e));
							}
						}
					}
					
					if(startDate!=null && stopDate!=null)
					{
						nacao.setValidPeriod("自"+startDate+"至"+stopDate);
					}
				}
			}
//			switchToSearchPage();
		}
		catch (Exception e) 
		{
			logger.info(SysConfig.getError(e));
			nacao.setUpdateStatus(3);
//			quitDriver();
//			initDriver();
//			switchToSearchPage();
		}
		switchToSearchPage();
		return nacao;
	}
	
	public boolean switchToDetailPage()
	{
		boolean res=false;
		try
		{
			for(String windowHandle:driver.getWindowHandles())
			{
				if(!windowHandle.equals(searchWindowHandle))
				{
					driver.switchTo().window(windowHandle);
					String curUrl=driver.getCurrentUrl();
					if(curUrl.contains("specialResult.html"))
					{
						res=true;
						break;
					}
				}
			}
		}
		catch (NoSuchWindowException e)
		{
			//
		}
		return res;
	}
	public boolean switchToSearchPage()
	{
		boolean res=false;
		try
		{
			String curWindowHandle;
			curWindowHandle=driver.getWindowHandle();
			for(String windowHandle:driver.getWindowHandles())
			{
				if(!windowHandle.equals(searchWindowHandle))
				{
					if(!windowHandle.equals(curWindowHandle))
					{
						driver.switchTo().window(windowHandle);
					}
					driver.close();
				}
			}
		}
		catch (NoSuchWindowException e)
		{
			//
		}
		driver.switchTo().window(searchWindowHandle);
		return res;
	}
	public Boolean waitForYmWindow()
	{
		return (new WebDriverWait(driver,1)).until(new ExpectedCondition<Boolean>() 
		{
			public Boolean apply(WebDriver d) 
        	{
				return d.findElement(ymWindowXpath)!=null;
        	}  	
        });
	}
//	public boolean switchToSearchPage() throws Exception
//	{
//		boolean flag=true;
//		driver.navigate().back();
//		return flag;
//	}
	
	public static void main(String[] args) throws Exception
	{
		NacaoOrgSearcher_2 searcher= new NacaoOrgSearcher_2();
		searcher.setLogger(new Logger("test"));
		searcher.initDriver();
//		System.out.println(searcher.search("802100433"));
//		System.out.println(System.currentTimeMillis());
//		System.out.println(searcher.search("802100433"));
		String[] codeArray={"802100433","596247871","59502609X","808220081","67452250X","574064548","228560207",
    			"669084461","500011128","579539434","576652132"};
		for(String code:codeArray)
		{
			NACAO nacao=searcher.search(code);
			searcher.logger.info(nacao.toString());
//			break;
		}
	}

}
