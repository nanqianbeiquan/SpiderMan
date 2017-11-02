package nacao;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import tools.HeadlessSearcher;
import tools.Logger;
import tools.SearcherFirefox;
import tools.SearcherPhantomJS;
import tools.SysConfig;

public class NacaoOrgSearcher extends SearcherFirefox
{
	WebElement validateIframe=null;
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
	
	WebElement validateInput=null;
	By validateInputXpath=By.xpath(".//*[@id='validateCodeId']");
	WebElement validateSubmit=null;
	By validateSubmitXpath=By.xpath("html/body/div[1]/table/tbody/tr[3]/td[3]/input");

	WebElement ymWindow=null;
	By ymWindowXpath=By.xpath("/html/body/div[1]/div[2][contains(@style,'display: none')]");

	SimpleDateFormat sdf1=new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat sdf2=new SimpleDateFormat("yyyy��M��d��");
	private String curCode;

	public NacaoOrgSearcher()
	{
		startUrl="https://s.nacao.org.cn";
	}
	
	public NACAO search(String orgCode) throws Exception
	{
		curCode=orgCode;
		NACAO nacao= new NACAO(curCode);
		try
		{
			//�ύ��ѯ����
			logger.info("��λ���������...");
			orgCodeInput=waitForWebElement(orgCodeInputXpath);
			logger.info("��λ�����ύ��ť...");
			orgCodeSubmit=waitForWebElement(orgCodeSubmitXpath);
			logger.info("�����ѯ����...");
			orgCodeInput.clear();
			orgCodeInput.sendKeys(curCode);
			logger.info("clicking submit button...");
			logger.info("����ύ��ť...");
			orgCodeSubmit.click();
			logger.info("�л�����ѯ���ҳ...");
			switchToDetailPage();
			try
			{
				validateIframe=waitForWebElement(validateIframeXpath);
			}
			catch(TimeoutException e2)
			{
				logger.info("waiting for validate iframe failed!");
				WebElement res = waitForWebElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td"));
				String resText=res.getText().trim();
				if(resText.startsWith("�����ڵ�IP��ʱ���ܽ��м���"))
				{
					nacao.setUpdateStatus(4);
					return nacao;
				}
				else
				{
					nacao.setUpdateStatus(3);
					switchToSearchPage();
					return nacao;
				}
			}
			driver.switchTo().frame(validateIframe);
			List<WebElement> baskets = waitForWebElements(By.xpath(".//div[@class='baskets']/div[@id]"));
			for(WebElement basket:baskets)
			{
				String basketId=basket.getAttribute("id");
				String letterId=basketId.replace("basket","letter");
				WebElement letter = waitForWebElement(By.xpath(String.format(".//div[@class='letters']/div[@id='%s']", letterId)));
				Actions action = new Actions(driver);
				action.dragAndDrop(letter, basket);
				action.perform();
			}
			((JavascriptExecutor)driver).executeScript("validateCode()");
			driver.switchTo().defaultContent();
			
			WebElement loadResult=waitForWebElement(loadResultTdXpath);
			if("�������0��".equals(loadResult.getText().trim()))
			{
				nacao.setUpdateStatus(0);
			}
			else
			{
				nacao.setUpdateStatus(1);
				nacao.setCertificateExists(0);
				//��������
				WebElement nameEle = loadResult.findElement(By.xpath("following-sibling::td[1]"));
				nacao.setOrgName(nameEle.getText().trim());
				//���
				WebElement registerNbrEle = loadResult.findElement(By.xpath("following-sibling::td[2]"));
				nacao.setRegisteredCode(registerNbrEle.getText().trim());
				//֤��
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
						nacao.setValidPeriod("��"+startDate+"��"+stopDate);
					}
				}
			}
//			switchToSearchPage();
		}
		catch (Exception e) 
		{
			logger.info(SysConfig.getError(e));
			nacao.setUpdateStatus(3);
		}
		switchToSearchPage();
		return nacao;
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
			try
			{
				curWindowHandle=driver.getWindowHandle();
			}
			catch (UnhandledAlertException e)
			{
				driver.switchTo().alert().accept();
				curWindowHandle=driver.getWindowHandle();
			}
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
	
//	public boolean switchToSearchPage() throws Exception
//	{
//		boolean flag=true;
//		driver.navigate().back();
//		return flag;
//	}
	
	public static void main(String[] args) throws Exception
	{
		NacaoOrgSearcher searcher= new NacaoOrgSearcher();
		searcher.setLogger(new Logger("test"));
		searcher.initDriver();
//		System.out.println(searcher.search("802100433"));
		System.out.println(System.currentTimeMillis());
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

	@Override
	public boolean loadStartPage() throws IOException {
		// TODO Auto-generated method stub
		boolean res =true;
		try
		{
			driver.get(startUrl);
			if(!driver.getTitle().equals("ȫ����֯����������Ϣ�˲�"))
			{
				res=false;
			}
		}
		catch (TimeoutException e)
		{
			res=false;
		}
		return res;
	}
}
