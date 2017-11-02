package nacao;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import tools.Logger;
import tools.SearcherFirefox;
import tools.SysConfig;

public class NacaoOrgSearcherTest extends SearcherFirefox{

	String searchPage="https://s.nacao.org.cn";
	
	By validateIframeXpath=By.xpath(".//*[@id='ym-ml']/div/div/div/iframe");
	WebElement orgCodeInput=null;
	By orgCodeInputXpath=By.xpath(".//input[@name='tit0']");
		
	WebElement orgCodeSubmit=null;
	By orgCodeSubmitXpath=By.xpath(".//a[@onclick='submitForm(0)']/input[@type='button']");

	By loadResultTdXpath=By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[@align][not(@valign)]");

	WebElement certificateIframe=null;
	By certificateIframeXpath=By.xpath(".//*[@id='highslide-wrapper-0']/div[1]/div/div/div[2]/iframe");
	
	
	Runtime runtime=Runtime.getRuntime();
	int iframeX,iframeY;
	String curOrgCode=null;
	
	WebElement validateInput=null;
	By validateInputXpath=By.xpath(".//*[@id='validateCodeId']");
	WebElement validateSubmit=null;
	By validateSubmitXpath=By.xpath("html/body/div[1]/table/tbody/tr[3]/td[3]/input");
	
	WebElement ymWindow=null;
	By ymWindowXpath=By.xpath("/html/body/div[1]/div[2][contains(@style,'display: none')]");
	
	FirefoxProfile profile = new FirefoxProfile();
	
	SimpleDateFormat sdf1=new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat sdf2=new SimpleDateFormat("yyyy年M月d日");

	public NacaoOrgSearcherTest() throws IOException
	{
		profile.setPreference("startup.homepage_welcome_url.additional",searchPage);
	}
	
	public int initDriver() throws Exception
	{
		logger.info("Driver is initializing...");
		return initDriver(1);
	}
	public int initDriver(int t) throws Exception
	{
		if(t==SysConfig.MAX_TRY_TIMES)
		{
			logger.info("Driver initializing failed!");
			logger.close();
			System.exit(1);
		}
		try
		{
			if(fireFoxPath!=null && !fireFoxPath.equals("default"))
			{
				System.setProperty("webdriver.firefox.bin",fireFoxPath);
			}
			profile.setAcceptUntrustedCertificates(true);
			profile.setAssumeUntrustedCertificateIssuer(true);
			driver=new FirefoxDriver(profile);
			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			driver.manage().window().maximize();
			
			searchWindowHandle=driver.getWindowHandle();
			orgCodeInput=waitForOrgCodeInput();
			orgCodeSubmit=waitForOrgCodeSubmit();
			
			logger.info("Driver initializing succeed!");
			return 0;
		}
		catch (TimeoutException e){
			logger.info(SysConfig.getError(e));
			driver.navigate().refresh();
		}
		catch (WebDriverException e)
		{
			logger.info(SysConfig.getError(e));
		}
		catch (Exception e)
		{
			logger.info(SysConfig.getError(e));
//			return 1;
		}
		return initDriver(t+1);
	}

	public NACAO search(String orgCode) throws Exception
	{
		curOrgCode=orgCode;
		NACAO nacao= new NACAO(curOrgCode);
		try
		{
			//提交查询请求
			orgCodeInput.clear();
			orgCodeInput.sendKeys(curOrgCode);
			
			File file = new File("F:\\EclipseProjects\\SpiderMan\\conf\\NacaoOrgJS.txt");
			Long filelength = file.length(); 
			byte[] filecontent = new byte[filelength.intValue()];
			InputStream inputStream = new FileInputStream(file);
			inputStream.read(filecontent);
			inputStream.close();
			String js0=new String(filecontent);
			
			String jsCode=js0+String.format(";js_0(\"%s\")", curOrgCode);
			((JavascriptExecutor)driver).executeScript(jsCode);
//			("var aeskey=\"phabro\";alert("
//					+ "x=\"+(Crypto.AES.encrypt(document.getElementById(\"lastxzqh\").value,aeskey)))");
//					+ "+\"&k=\"+(Crypto.AES.encrypt(document.getElementById(\"kind\").value,aeskey))"
//					+ "+\"&s=\"+(Crypto.AES.encrypt(document.getElementById(\"strfind\").value, aeskey))"
//					+ "+\"&y=\"+(Crypto.AES.encrypt(document.getElementById(\"key\").value,aeskey)))");
			
			System.exit(1);
			
			orgCodeSubmit.click();
			
			//跳转页面
			
			if(switchToSearchResultWindow()==false)
			{
				nacao.setUpdateStatus(2); //提交查询请求异常
//				quitDriver();
//				initDriver();
				return nacao;
			}
			
			WebElement validateIframe=null;
			try
			{
				validateIframe=waitForWebElement(validateIframeXpath);
			}
			catch(TimeoutException e)
			{
				WebElement res = waitForWebElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td"));
				String resText=res.getText().trim();
				if(resText.startsWith("您所在的IP暂时不能进行检索"))
				{
					nacao.setUpdateStatus(4);
					return nacao;
				}
			}
			driver.switchTo().frame(validateIframe);
			//破解验证码
			By basketsXpath=By.xpath("//*[@id='baskets_0']/div[@id]");
			List<WebElement> baskets = waitForWebElements(basketsXpath);			
			for(WebElement basket:baskets)
			{
				String basketId=basket.getAttribute("id");
				String letterId=basketId.replace("basket", "letter");
				WebElement letter = waitForWebElement(By.xpath(String.format("//*[@id='letters_0']/div[@id='%s']",letterId)));
				Actions action = new Actions(driver);
				action.dragAndDrop(letter, basket).perform();
			}
			WebElement yzmSubmit = waitForWebElement(By.xpath("/html/body/div[1]/form/input[1]"));
			yzmSubmit.click();
			driver.switchTo().defaultContent();
			WebElement loadResult=waitForLoadResult();
			if("检索结果0条".equals(loadResult.getText().trim()))
			{
				nacao.setUpdateStatus(1);
			}
			else
			{
				nacao.setUpdateStatus(0);
				nacao.setCertificateExists(0);
				//机构名称
//				WebElement nameEle = driver.findElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[2]"));
				WebElement nameEle = loadResult.findElement(By.xpath("following-sibling::td[1]"));
				nacao.setOrgName(nameEle.getText().trim());
				//编号
//				WebElement registerNbrEle = driver.findElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[3]"));
				WebElement registerNbrEle = loadResult.findElement(By.xpath("following-sibling::td[2]"));
				nacao.setRegisteredCode(registerNbrEle.getText().trim());
				//证书
//				WebElement certificateEle = driver.findElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[4]"));
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
								logger.info(SysConfig.getError(e));
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
								logger.info(SysConfig.getError(e));
							}
							
						}
						else if("reservea".equals(info[0]) && !"null".equals(info[1]))
						{
							nacao.setReservea(info[1].trim());
						}
					}
					
					if(startDate!=null && stopDate!=null)
					{
						nacao.setValidPeriod("自"+startDate+"至"+stopDate);
					}
				}
			}
			driver.close();
			switchToSearchPage();
		}
		catch (UnreachableBrowserException e) //浏览器崩溃
		{
			logger.info(SysConfig.getError(e));
			nacao.setUpdateStatus(6);
		}
		catch (TimeoutException e) //超时
		{
			logger.info(SysConfig.getError(e));
			nacao.setUpdateStatus(3);
			logger.info("Closing driver...");
			driver.close();
			logger.info("switchTo searchWindowHandle...");
			switchToSearchPage();
//			driver.switchTo().window(searchWindowHandle);
		}
		catch (StaleElementReferenceException e)
		{
			logger.info(SysConfig.getError(e));
			nacao.setUpdateStatus(3);
			logger.info("Closing driver...");
			driver.close();
			logger.info("switchTo searchWindowHandle...");
			switchToSearchPage();
//			driver.switchTo().window(searchWindowHandle);
		}
		catch (UnhandledAlertException e)
		{
			logger.info(SysConfig.getError(e));
			nacao.setUpdateStatus(2); //提交查询请求异常
			quitDriver();
			initDriver();
		}
		catch (RasterFormatException e)
		{
			logger.info(SysConfig.getError(e));
			nacao.setUpdateStatus(3);
			logger.info("Closing driver...");
			driver.close();
			logger.info("switchTo searchWindowHandle...");
			switchToSearchPage();
//			driver.switchTo().window(searchWindowHandle);
		}
		catch (WebDriverException e)
		{
			logger.info(SysConfig.getError(e));
			nacao.setUpdateStatus(3); 
			logger.info("WebDriverException,web driver will be rebuilt...");
			quitDriver();
			initDriver();
		}
		catch (Exception e) //未知错误
		{
			logger.info(SysConfig.getError(e));
			nacao.setUpdateStatus(7);
		}
		return nacao;
	}
	
	
	public WebElement waitForOrgCodeInput()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(orgCodeInputXpath);
        	}
        });
	}
	
	public WebElement waitForOrgCodeSubmit()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(orgCodeSubmitXpath);
        	}
        });
	}
	
	public WebElement waitForValidateInput()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(validateInputXpath);
        	}        	       	
        });
	}
	
	public WebElement waitForValidateSubmit()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(validateSubmitXpath);
        	}        	       	
        });
	}
	
	public WebElement waitForCertificateIframe()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(certificateIframeXpath);
        	}        	       	
        });
	}
	
	public WebElement waitForValidateIframe()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(validateIframeXpath);
        	}        	       	
        });
	}
	
	public WebElement waitForLoadResult()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS)).until(new ExpectedCondition<WebElement>() 
		{
			public WebElement apply(WebDriver d) 
        	{
				return d.findElement(loadResultTdXpath);
        	}        	       	
        });
	}

	public Boolean waitForYmWindow()
	{
		return (new WebDriverWait(driver,2)).until(new ExpectedCondition<Boolean>() 
		{
			public Boolean apply(WebDriver d) 
        	{
				return d.findElement(ymWindowXpath)!=null;
        	}        	       	
        });
	}
	
	public boolean switchToSearchPage() throws Exception
	{
		boolean flag=true;
		try
		{
			driver.switchTo().window(searchWindowHandle);
		}
		catch (NoSuchWindowException e)
		{
			quitDriver();
			initDriver();
		}
		return flag;
	}
	
	public List<WebElement> waitForWebElements(final By eleXpath)
	{
		waitForWebElement(eleXpath);
		return driver.findElements(eleXpath);
	}
	
	public boolean switchToSearchResultWindow() throws IOException{  
	    boolean flag = false;  
	    try {  
//	        String currentHandle = driver.getWindowHandle();  
	        Set<String> handles = driver.getWindowHandles();  
	        for (String s : handles) {  
	            if (s.equals(searchWindowHandle))  
	                continue;
	            else {
	                driver.switchTo().window(s);  
	                flag = true;
                    break;    
	            }  
	        }  
	    } catch (NoSuchWindowException e) {  
	    	logger.info(SysConfig.getError(e));
	        flag = false;  
	    }  
	    return flag;  
	}  

	//截图获取验证码
	public void screenShot(int i) throws IOException
	{
		WebDriver augmentedDriver = new Augmenter().augment(driver);
		byte[] takeScreenshot = ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.BYTES);
		BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(takeScreenshot));
		ImageIO.write(originalImage, "png", new File("F:\\EclipseProjects\\SpiderMan\\data\\validate\\"+curOrgCode+"_"+i+".png"));
	}


	public static void main(String[] args) throws Exception
	{
		
		NacaoOrgSearcherTest searcher= new NacaoOrgSearcherTest();
		searcher.setLogger(new Logger("test"));
		searcher.setFireFoxPath("C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe");
		searcher.initDriver();
//		System.out.println(searcher.search("802100433"));
		System.out.println(System.currentTimeMillis());
//		System.out.println(searcher.search("802100433"));
		String[] codeArray={"802100433","596247871","59502609X","808220081","67452250X","574064548","228560207",
    			"669084461","500011128","579539434","576652132"};
		for(String code:codeArray)
		{
			NACAO nacao=searcher.search(code);
			System.out.println(nacao);
//			break;
//			String[] colsAndVals=nacao.getColsAndVals();
//			colsAndVals[0]+=",lastUpdateTime";
//			colsAndVals[1]+=",getDate()";
//			String insertSql=String.format("insert into %s(%s) values(%s)","NacaoOrg",colsAndVals[0],colsAndVals[1]);
//			logger.info(insertSql);
//			dbClient.execute(insertSql);
		}
	}

	@Override
	public boolean loadStartPage() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}
}
