package gs;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
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
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.UnreachableBrowserException;

import tools.Logger;
import tools.MSSQLClient;
import tools.SearcherFirefox;
import tools.SysConfig;

public class LnGsSearcher extends SearcherFirefox {
	
	public String searcherUrl="http://gsxt.lngs.gov.cn/saicpub/";
	public String validateUrl="";
	public String curCode;
	public String detailPageTitle="市场主体信用信息公示系统-首页";
	public String detailShareholderInfoPageTitle="投资人及出资信息";
	FirefoxProfile profile = new FirefoxProfile();
	
	WebElement searchFrame;
	By searchFrameBy=By.tagName("Frame");
	
	WebElement codeInput;
	By codeInputXpath=By.xpath("//*[@id='solrCondition']");
	WebElement validateImage;
	By validateImageXpath=By.xpath("//*[@id='jcaptcha']");
	WebElement validateInput;
	By validateInputXpath=By.xpath("//*[@id='authCode-test']");
	WebElement listContent;
	By listContentXpath=By.xpath("//*[@id='listContent']");
	WebElement openDeatil;
	By openDetailXpath=By.xpath("//a[contains(@onClick,'openDetail')]");
	WebElement abstractInfo;
	By abstractInfoXpath=By.xpath("//li[@class='font14']");

	HtmlTable[] tableArr;
	int searchFrameX,searchFrameY;
	int allowNullWaitInSeconds;
	public MSSQLClient dbClient;
	
	public LnGsSearcher() throws JDOMException, IOException
	{
		profile.setPreference("startup.homepage_welcome_url.additional","http://gsxt.lngs.gov.cn/saicpub/");
		loadHtmlTableConfig(SysConfig.workDir+"\\GsTables.xml");
	}
	
	public int initDriver() throws Exception
	{
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
			System.out.println("build driver ...");
			driver=new FirefoxDriver(profile);
			System.out.println("build driver succeed!");
//			driver.manage().window().maximize();
			searchWindowHandle=driver.getWindowHandle();
			searchFrame=waitForWebElement(searchFrameBy);
			searchFrameX=searchFrame.getLocation().getX();
			searchFrameY=searchFrame.getLocation().getY();
			driver.switchTo().frame(searchFrame);
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
		}
		return initDriver(t+1);
	}
	
	public void setDbClient(MSSQLClient dbClient)
	{
		this.dbClient=dbClient;
	}
	
	//截图获取验证码
	public void screenShot(String code) throws IOException
	{
		Point imgLocation = validateImage.getLocation();
		Dimension imgSize = validateImage.getSize();
		
		WebDriver augmentedDriver = new Augmenter().augment(driver);
		byte[] takeScreenshot = ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.BYTES);
		BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(takeScreenshot));
		BufferedImage croppedImage = originalImage.getSubimage(
				searchFrameX+imgLocation.getX(),
				searchFrameY+imgLocation.getY(),
				imgSize.getWidth(),
				imgSize.getHeight());
		ImageIO.write(croppedImage, "png", new File(SysConfig.getValidateCodeSavePath(code)));
	}
		
	@Override
	public GsModel search(String code) throws Exception {
		
		curCode=code;
		GsModel model=new GsModel(code);
		try
		{
			codeInput=waitForWebElement(codeInputXpath);
			((JavascriptExecutor)driver).executeScript("arguments[0].value='"+code+"'", codeInput);
			((JavascriptExecutor)driver).executeScript("zdm()");		
			validateImage=waitForWebElement(validateImageXpath);
			validateInput=waitForWebElement(validateInputXpath);
			screenShot(code);
			String validateCode=recongnizeValidateCode(SysConfig.LIAONING_OCR, SysConfig.getValidateCodeSavePath(code));
//			System.out.println(code+" -> "+validateCode);
			((JavascriptExecutor)driver).executeScript("arguments[0].value='"+validateCode+"'", validateInput);
			((JavascriptExecutor)driver).executeScript("searchInfo()");
			try
			{
				listContent=waitForWebElement(listContentXpath);
			}
			catch (UnhandledAlertException e)
			{
				//验证码识别失败
				model.setUpdateStatus(5);
				return model;
			}
			
			if(listContent.getText().equals("")) 
			{
				//查询结果不存在
				model.setUpdateStatus(0);
				return model;
			}
			model.setUpdateStatus(1);
			
			openDeatil=waitForWebElement(listContent, openDetailXpath);
			abstractInfo=waitForWebElement(listContent, abstractInfoXpath);
			model.setAbstractInfo(abstractInfo.getText());

			//基本信息
			openDeatil.click();
			if(switchToPage(detailPageTitle)==false)
			{
				model.setUpdateStatus(4);
				return model;
			}
			int step=0;
			while(step<tableArr.length)
			{
				if(tableArr[step].loadTable(code)==0)
				{
					step++;
				}
				else
				{
					step=tableArr[step].nextStep;
				}
			}
			driver.close();
			driver.switchTo().window(searchWindowHandle);
			driver.switchTo().frame(searchFrame);
			model.setUpdateStatus(1);
		}
		catch (UnreachableBrowserException e) //浏览器崩溃
		{
			logger.info(SysConfig.getError(e));
			model.setUpdateStatus(6);
		}
		catch (TimeoutException e) //超时
		{
			logger.info(SysConfig.getError(e));
			model.setUpdateStatus(3);
			switchToSearchPage();
//			driver.switchTo().window(searchWindowHandle);
		}
		catch (StaleElementReferenceException e)
		{
			logger.info(SysConfig.getError(e));
			model.setUpdateStatus(3);
			switchToSearchPage();
//			driver.switchTo().window(searchWindowHandle);
		}
		catch (WebDriverException e)
		{
			logger.info(SysConfig.getError(e));
			model.setUpdateStatus(3); 
//			logger.info("WebDriverException,web driver will be rebuilt...");
			switchToSearchPage();
		}
		catch (IllegalArgumentException e)
		{
			logger.info(SysConfig.getError(e));
			model.setUpdateStatus(8); 
		}
		catch (Exception e) //未知错误 
		{
			logger.info(SysConfig.getError(e));
			model.setUpdateStatus(7);
		}
		
		return model;
	}
	
	public boolean switchToPage(String title)
	{
		boolean res=false;
		for(String handle:driver.getWindowHandles())
		{
			driver.switchTo().window(handle);
			if(driver.getTitle().equals(title))
			{
				res=true;
				break;
			}
		}
		return res;
	}

	public void switchToSearchPage() throws Exception
	{
		try
		{
			for(String handle:driver.getWindowHandles())
			{
				if(!handle.equals(searchWindowHandle))
				{
					driver.switchTo().window(handle);
					driver.close();
				}
			}
			driver.switchTo().window(searchWindowHandle);
			driver.switchTo().frame(searchFrame);
		}
		catch (NoSuchWindowException e)
		{
			quitDriver();
			initDriver();
		}
	}
	
	public void loadHtmlTableConfig(String configPath) throws JDOMException, IOException
	{
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(new File(configPath));
		
		Element rootEle = doc.getRootElement();
		
		int tableNums=Integer.valueOf(rootEle.getChild("tableNums").getText());
		
		tableArr=new HtmlTable[tableNums];

		List<Element> tableList = rootEle.getChildren("table");
		for(Element table:tableList)
		{
			String tableDesc=table.getChildText("desc");
			String tableName=table.getChildText("name");
			String by=table.getChildText("by");
			String js=table.getChildText("js");
			
			int colNums=-1;
			if(by.equals("idx"))
			{
				colNums=Integer.valueOf(table.getChildText("colNums"));
			}
			int step=Integer.valueOf(table.getChildText("step"));
			int nextStep=table.getChildText("nextStep")==null?(step+1):Integer.valueOf(table.getChildText("nextStep"));
			int waitInSeconds=Integer.valueOf(table.getChildText("waitInSeconds"));
			tableArr[step]=new HtmlTable(tableDesc,tableName,by,colNums,nextStep,waitInSeconds);
			tableArr[step].setJs(js);
			
			List<Element> columns = table.getChildren("column");
			
			if(by.equals("desc"))
			{
				tableArr[step].columnMap=new HashMap<String,String>();
				for(Element column:columns)
				{
					String columnDesc = column.getAttributeValue("desc");
					String columnName = column.getText();
					tableArr[step].columnMap.put(columnDesc,columnName);
				}
			}
			else if(by.equals("idx"))
			{
				
				tableArr[step].columnArr=new String[columns.size()];
				for(Element column:columns)
				{
					int columnIdx=Integer.valueOf(column.getAttributeValue("idx"));
					String columnName = column.getText();
					tableArr[step].columnArr[columnIdx]=columnName;
				}
			}
			if(table.getChildText("subColNums")!=null)
			{
				tableArr[step].subColumnArr=new String[Integer.valueOf(table.getChildText("subColNums"))];
				List<Element> subColumns = table.getChildren("subColumn");
				for(Element subColumn:subColumns)
				{
					int subColumnIdx=Integer.valueOf(subColumn.getAttributeValue("idx"));
					String subColumnName=subColumn.getText();
					tableArr[step].subColumnArr[subColumnIdx]=subColumnName;
				}
			}
		}
	}
	
	class HtmlTable
	{
		public String tableName;
		public String tableDesc;
		public int colNums;
		public HashMap<String,String> columnMap;
		public String[] columnArr;
		public String[] subColumnArr;
		public int nextStep;
		public String by;
		public String js=null;
		public int waitInSeconds;
		
		public HtmlTable(String tableDesc,String tableName,String by,int colNums,int nextStep,int waitInSeconds)
		{
			this.tableDesc=tableDesc;
			this.tableName=tableName;
			this.by=by;
			this.colNums=colNums;
			this.nextStep=nextStep;
			this.waitInSeconds=waitInSeconds;
		}
		
		public void setJs(String js)
		{
			this.js=js;
		}
		
		public int loadTable(String code) throws SQLException, InterruptedException, IOException 
		{
			String deleteCmd=String.format("delete from %s where RegistrationNo='%s'", tableName,code);
			dbClient.statement.addBatch(deleteCmd);
			WebElement table;
			if(js!=null)
			{
				((JavascriptExecutor)driver).executeScript(js);
			}
			try
			{
				table = waitForWebElement(By.xpath(String.format("//th[normalize-space(text())='%s']/../../..", tableDesc)));
//				System.out.println(tableDesc+" 加载成功!");
			}
			catch (TimeoutException e)
			{
				System.out.println(tableDesc+" 不存在!");
				return 0;
			}
			if(by.equals("desc"))
			{
				parseTable1(table);
			}
			else if(by.equals("idx"))
			{
				parseTable2(table);
			}
			return 1;
		}
		
		public void parseTable1(WebElement table) throws SQLException
		{
			List<WebElement> rows = table.findElements(By.xpath("tbody/tr"));
			StringBuilder colBuilder=new StringBuilder();
			StringBuilder valBuilder=new StringBuilder();
			for(WebElement row:rows)
			{
				List<WebElement> cols = row.findElements(By.xpath("th"));
				List<WebElement> vals = row.findElements(By.xpath("td"));
				if(cols.size()==vals.size())
				{
					for(int i=0;i<cols.size();i++)
					{
						if(!cols.get(i).getText().trim().equals(""))
						{
							String col = columnMap.get(cols.get(i).getText().trim());
							if(col==null)
							{
								throw new IllegalArgumentException("基本信息列名未知");
							}
							colBuilder.append(col+",");
							String val = vals.get(i).getText().trim();
							valBuilder.append("'"+val+"',");
						}
					}
				}
			}
			String insertCmd=String.format("insert into %s(%s province,lastUpdateTime) values(%s '辽宁',getDate())",tableName,colBuilder.toString(),valBuilder.toString());
//			System.out.println(insertCmd);
			dbClient.statement.addBatch(insertCmd);
		}
		
		public void parseTable2(WebElement table) throws InterruptedException, SQLException, IOException
		{
			String style="";
			try
			{
				if(tableDesc.equals("清算信息"))
				{
					waitForWebElement(table, By.xpath("tbody[1]/tr[position()>2][position()<last()]/td[not(text()='')]"),waitInSeconds);
					style=table.findElement(By.xpath("tbody[1]/tr[position()>2][position()<last()]")).getAttribute("style");
				}
				else
				{
					waitForWebElement(table, By.xpath("tbody[@id]/tr[1]/td[not(text()='')]"),waitInSeconds);
					style=table.findElement(By.xpath("tbody[@id]/tr[1]")).getAttribute("style");
				}
//				Thread.sleep(500);
			}
			catch (TimeoutException e){}
			
			List<WebElement> rows;
			if(tableDesc.equals("清算信息"))
			{
				rows = table.findElements(By.xpath("tbody[1]/tr[position()>2][position()<last()]"));
			}
			else
			{
				rows = table.findElements(By.xpath("tbody[@id]/tr"));
			}
			
			StringBuilder colBuilder=new StringBuilder(),valBuilder=new StringBuilder();
			for(WebElement row:rows)
			{
				if(row.getAttribute("style").contains("display: none"))
				{
					((JavascriptExecutor)driver).executeScript(String.format("arguments[0].style='%s'",style), row);
				}
				for(int t=0;t<SysConfig.MAX_TRY_TIMES;t++)
				{
//					System.out.println("*"+row.getText().trim()+"*");
					if(!row.getText().trim().equals(""))
					{
						break;
					}
					else
					{
//						System.out.println("Thread.sleep(1000)...");
						Thread.sleep(1000);
					}
				}
				List<WebElement> vals = row.findElements(By.xpath("td"));
				for(int i=0,l=vals.size()-vals.size()%colNums;i<l;i++)
				{
					boolean moreFlag;
					WebElement val = vals.get(i);
					try
					{
						val.findElement(By.linkText("更多")).click();
						moreFlag=true;
					}
					catch (NoSuchElementException e)
					{
						moreFlag=false;
					}
					if(tableName.equals("Shareholder_Info") && i==4)
					{
						try
						{
							for(int t=0;t<SysConfig.MAX_TRY_TIMES;t++)
							{
								try
								{
									val.findElement(By.linkText("详情")).click();
									if(switchToPage(detailShareholderInfoPageTitle)==true)
									{
										break;
									}
								}
								catch (StaleElementReferenceException e)
								{
									switchToPage(detailShareholderInfoPageTitle);
								}
							}
							try
							{
								waitForWebElement(By.xpath("/html/body/div[2]/div/div/table/tbody/tr[4]/td[not(text()='')]"),3);
								List<WebElement> subVals = driver.findElements(By.xpath("/html/body/div[2]/div/div/table/tbody/tr[4]/td[not(contains(@style,'display:none'))]"));
								for(int j=0;j<8;j++)
								{
									colBuilder.append(subColumnArr[j]+",");
									valBuilder.append("'"+subVals.get(j+1).getText()+"',");
								}
							}
							catch (TimeoutException e)
							{
								logger.info("详情信息不存在。");
							}
							driver.close();
							switchToPage(detailPageTitle);
						}
						catch (NoSuchElementException e){}
					}
					else
					{
						String valStr = val.getText().trim();
						if(moreFlag)
						{
							valStr=valStr.substring(0, valStr.length()-4);
						}
						colBuilder.append(columnArr[i]+",");
						valBuilder.append("'"+valStr+"',");
					}
					
					if((i+1)%colNums==0)
					{
						String insertCmd=String.format("insert into %s(%sRegistrationNo,lastUpdateTime) values(%s'%s',getDate())",tableName,colBuilder.toString(),valBuilder.toString(),curCode);
//						System.out.println(insertCmd);
						dbClient.statement.addBatch(insertCmd);
						colBuilder.delete(0, colBuilder.length());
						valBuilder.delete(0, valBuilder.length());
					}
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		MSSQLClient client = new MSSQLClient(
				String.format("jdbc:sqlserver://%s:1433;DatabaseName=%s",SysConfig.MSSQL_HOST,SysConfig.MSSQL_DB),
				SysConfig.MSSQL_USER, //user
				SysConfig.MSSQL_PWD, //pwd
				false //autoCommit
				);
		LnGsSearcher searcher= new LnGsSearcher();
		searcher.setDbClient(client);
		searcher.setLogger(new Logger("test"));
		searcher.setFireFoxPath("C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe");
		searcher.initDriver();
		
		String[] codeArr=new String[]{"210000004918831","210123000021460","210123000004123","210106000058876","210123000017434","210123000028277","210123000024675"};
		for(String code:codeArr)
		{
			GsModel model = searcher.search(code);
			String updateCmd="update GsSrc set "+model+" where registeredCode='"+code+"'";
			System.out.println(new Date());
			System.out.println(updateCmd);
			break;
		}
		searcher.dbClient.statement.executeBatch();
		try
		{
			searcher.dbClient.commit();
		}
		catch (BatchUpdateException e)
		{
			System.out.println(e.getSQLState());
		}
	}

	@Override
	public boolean loadStartPage() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}
}