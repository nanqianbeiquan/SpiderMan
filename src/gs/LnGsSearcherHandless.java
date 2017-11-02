package gs;

import java.io.File;
import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import tools.HeadlessSearcher;
import tools.Logger;
import tools.MSSQLClient;
import tools.SysConfig;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class LnGsSearcherHandless extends HeadlessSearcher {
	
	public String province="����";
	public String detailPageTitle="�г�����������Ϣ��ʾϵͳ-��ҳ";
	public String detailShareholderInfoPageTitle="Ͷ���˼�������Ϣ";
	WebElement codeInput;
	By codeInputXpath=By.xpath("//*[@id='solrCondition']");
	WebElement validateInput;
	By validateInputXpath=By.xpath("//*[@id='authCode-test']");
	WebElement listContent;
	By listContentXpath=By.xpath("//*[@id='listContent']");
	WebElement openDeatil;
	By openDetailXpath=By.xpath("//a[contains(@onClick,'openDetail')]");
	WebElement abstractInfo;
	By abstractInfoXpath=By.xpath("//li[@class='font14']");

	HashMap<String,HtmlTable> tableMap=new HashMap<String,HtmlTable>();

	public MSSQLClient dbClient;
	public GsModel model;
	
	
	public LnGsSearcherHandless() throws JDOMException, IOException
	{
		searchUrl="http://gsxt.lngs.gov.cn/saicpub/entPublicitySC/entPublicityDC/entPublicity/search/searchmain.jsp";
		validateUrl="http://gsxt.lngs.gov.cn/saicpub/commonsSC/loginDC/securityCode.action";
		loadHtmlTableConfig(SysConfig.workDir+"\\GsTables.xml");
	}
	
	public void setDbClient(MSSQLClient dbClient)
	{
		this.dbClient=dbClient;
	}

	@Override
	public GsModel search(String code) throws Exception {
		
		curCode=code;
		model=new GsModel(code);
		
		submitSearchRequest();
		if(model.updateStatus==1)
		{
			loadDetailInfo();
		}
		return model;
	}
	
	public void submitSearchRequest() throws IOException
	{
		try
		{
			codeInput=waitForWebElement(codeInputXpath);
			((JavascriptExecutor)driver).executeScript("arguments[0].value='"+curCode+"'", codeInput);
			((JavascriptExecutor)driver).executeScript("zdm()");
			validateInput=waitForWebElement(validateInputXpath);
			String validateCode=recongnizeValidateCode(SysConfig.LIAONING_OCR,validateUrl);
			logger.info("validateCode:"+validateCode);
			((JavascriptExecutor)driver).executeScript("arguments[0].value='"+validateCode+"'", validateInput);
			((JavascriptExecutor)driver).executeScript("searchInfo()");
			listContent=waitForWebElement(listContentXpath);
			if(listContent.getText().equals("")) 
			{
				
				//��ѯ���������
				model.setUpdateStatus(0);
			}
			else
			{
				model.setUpdateStatus(1);
				openDeatil=waitForWebElement(listContent, openDetailXpath);
				abstractInfo=waitForWebElement(listContent, abstractInfoXpath);
				model.setAbstractInfo(abstractInfo.getText());
				//������Ϣ
				openDeatil.click();
			}
		}
		catch (Exception e)
		{
			logger.info(SysConfig.getError(e));
			logger.info("�ύ��ѯ����ʧ�ܡ�");
			model.setUpdateStatus(7);
		}
	}
	
	public void loadDetailInfo() throws Exception
	{
		try
		{
			String tableJs=null;
			if(switchToPage(detailPageTitle)==false)
			{
				logger.info("��������ҳʧ��");
				model.setUpdateStatus(7);
				return;
			}
			
			List<WebElement> gsTabs = waitForWebElements(By.xpath("/html/body/div[2]/div[2]/div/div[1]/ul/li"));
			for(int i=0;i<gsTabs.size();i++)
			{
				
				String tabJs = gsTabs.get(i).getAttribute("onclick");
				String tabText=gsTabs.get(i).getText().trim();
				if(tabJs!=null && !tabJs.equals("rMethod('gs_dj'),changeStyle('gs_tabs',this)"))
				{
					((JavascriptExecutor)driver).executeScript(tabJs);
				}
				List<WebElement> divs=waitForWebElements(By.xpath(String.format("/html/body/div[2]/div[2]/div/div[%d]/div", i+2)));
				int tableIdx=0;
				boolean hasData;
				for(WebElement div:divs)
				{
					String tableDesc=null;					
					if(tabText.equals("�Ǽ���Ϣ") && (tableIdx++)==0)
					{
						hasData=true;
						tableDesc="������Ϣ";
					}
					else
					{
						hasData=false;
						try
						{
							tableDesc=waitForWebElement(div, By.xpath("table/*/tr/th[normalize-space(text())!='']")).getText();
						}
						catch (TimeoutException e)
						{
							if(div.getText().startsWith("������"))
							{
								logger.info("�����ˣ�");
								continue;
							}
							else
							{
								throw new TimeoutException();
							}
						}
						tableDesc=tableDesc.split("\n")[0];
						try
						{
							String layer1=div.getText();
							tableJs=layer1.substring(layer1.indexOf("$"),layer1.indexOf("});")+3);
							if(tableJs!=null && !tableJs.contains("[]"))
							{
								hasData=true;
							}
						}
						catch (IndexOutOfBoundsException e){}
					}
					if(hasData==true)
					{
						if(tableMap.get(tableDesc)==null)
						{
							logger.info(div.getText());
							throw new IllegalArgumentException("δ����ı�ṹ!\n code:"+curCode+"\n tableDesc:"+tableDesc);
						}
						else
						{
							tableMap.get(tableDesc).setJsCode(tableJs);
							tableMap.get(tableDesc).deleteFromDB();
							tableMap.get(tableDesc).parseTable(div);
						}
					}
				}
			}
			driver.close();
			switchToSearchPage();
		}
		catch (IllegalArgumentException e)
		{
			logger.info(SysConfig.getError(e));
			model.setUpdateStatus(8);
		}
		catch (Exception e) //δ֪���� 
		{
			logger.info(SysConfig.getError(e));
			model.setUpdateStatus(7);
		}
	}
	
	public boolean switchToPage(String title) throws InterruptedException
	{
		boolean res=false;
		for(String handle:driver.getWindowHandles())
		{
//			System.out.println("title:"+title);
			driver.switchTo().window(handle);
			if(driver.getTitle()==null)
			{
//				System.out.println("Title is null");
				driver.close();
			}
			else if(driver.getTitle().equals(title))
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
		List<Element> tableList = rootEle.getChildren("table");
		for(Element table:tableList)
		{
			String tableDesc=table.getChildText("desc");
			String tableName=table.getChildText("name");
			String by=table.getChildText("by");
			String pripidIdx=table.getChildText("pripidIdx");
			String detailUrl=table.getChildText("detailUrl");
			
			if(tableDesc.equals("������Ϣ"))
			{
				tableMap.put(tableDesc, new JiBenXinXi(tableDesc, tableName));
			}
			else if (tableDesc.equals("�ɶ���Ϣ"))
			{
				tableMap.put(tableDesc, new GuDongXinXi(tableDesc, tableName));
			}
			else if (tableDesc.equals("������Ϣ"))
			{
				tableMap.put(tableDesc, new QingSuanXinXi(tableDesc, tableName));
			}
			else if (tableDesc.equals("������Ѻ�Ǽ���Ϣ"))
			{
				tableMap.put(tableDesc,new DongChanDiYaDengJiXinXi(tableDesc, tableName));
			}
			else if(tableDesc.equals("��Ȩ���ʵǼ���Ϣ"))
			{
				tableMap.put(tableDesc,new GuQuanChuZhiDengJiXinXi(tableDesc, tableName));
			}
			else
			{
				tableMap.put(tableDesc,new HtmlTable(tableDesc,tableName));
			}
			
			tableMap.get(tableDesc).identity=Boolean.valueOf(table.getChildText("identity"));
			tableMap.get(tableDesc).pripidIdx=pripidIdx;
			tableMap.get(tableDesc).detailUrlTemplate=detailUrl;
			List<Element> columns = table.getChildren("column");

			if(by.equals("desc"))
			{
				tableMap.get(tableDesc).descColMap=new HashMap<String,String>();
				for(Element column:columns)
				{
					String columnDesc = column.getAttributeValue("desc");
					String columnName = column.getText();
					tableMap.get(tableDesc).descColMap.put(columnDesc,columnName);
				}
			}
			else if(by.equals("label"))
			{
				tableMap.get(tableDesc).labelColMap=new HashMap<String,String>();
				for(Element column:columns)
				{
					String columnLabel = column.getAttributeValue("label");
					String columnName = column.getText();
					tableMap.get(tableDesc).labelColMap.put(columnLabel, columnName);
				}
			}
		}
	}
	
	class HtmlTable
	{
		public String tableName;
		public String tableDesc;
		public HashMap<String,String> descColMap;
		public HashMap<String,String> labelColMap;
		public JSONObject labelValMap;
		public HashMap<String,String> colValMap=new HashMap<String,String>();
		public String pripidIdx=null;
		public String detailUrlTemplate=null;
		public boolean identity;
		public String jsCode=null;
		public String[] funcArgs;
		
		public HtmlTable(String tableDesc,String tableName)
		{
			this.tableDesc=tableDesc;
			this.tableName=tableName;
		}
		
		public void setJsCode(String jsCode)
		{
			this.jsCode=jsCode;
		}
		
		public void deleteFromDB() throws SQLException
		{
			String deleteCmd=String.format("delete from %s where RegistrationNo='%s'", tableName,curCode);
			dbClient.addBatch(deleteCmd);
		}
		public void parseTable(WebElement tableDiv) throws InterruptedException, SQLException, IOException
		{
			try
			{
//				System.out.println(tableDesc+":"+jsCode);
				String otherArgsStr=jsCode.substring(jsCode.lastIndexOf("]")+2);
				String[] otherArgsArr=StringUtils.split(otherArgsStr,",");
				funcArgs=new String[otherArgsArr.length+1];
				funcArgs[0]=jsCode.substring(jsCode.indexOf("[")+1,jsCode.lastIndexOf("]")).trim();
				for(int i=0;i<otherArgsArr.length;i++)
				{
					funcArgs[i+1]=otherArgsArr[i].substring(otherArgsArr[i].indexOf("\"")+1,otherArgsArr[i].lastIndexOf("\""));
				}
//				System.out.println(Arrays.asList(funcArgs));
				int startIdx=0,stopIdx=-1,rowNums=0;
				while(stopIdx!=(funcArgs[0].length()-1))
				{
					startIdx=funcArgs[0].indexOf("{\"", stopIdx);
					stopIdx=funcArgs[0].indexOf("\"}", startIdx);
					String jsonStr=funcArgs[0].substring(startIdx, stopIdx+2);
					labelValMap=JSON.parseObject(jsonStr);
					if(identity==true)
					{
						labelValMap.put("identity", String.valueOf(++rowNums));
					}
					if(detailUrlTemplate!=null)
					{
						String detailUrl=detailUrlTemplate;
						int idx1=0,idx2=0;
						while(idx2!=detailUrlTemplate.length()-1)
						{
							idx1=detailUrlTemplate.indexOf("(", idx2);
							idx2=detailUrlTemplate.indexOf(")", idx1);
							String arg=detailUrlTemplate.substring(idx1+1, idx2);
							if(arg.equals("pripid"))
							{
								String pripid=funcArgs[funcArgs.length+Integer.valueOf(pripidIdx)];
								detailUrl=detailUrl.replace("("+arg+")", pripid);
							}
							else
							{
								detailUrl=detailUrl.replace("("+arg+")", labelValMap.getString(arg));
							}
						}
						labelValMap.put("detailUrl", detailUrl);
					}
					updateColValMap();
					extraProc();
					updateToDB();
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				return;
			}
		}
		
		public void updateColValMap() throws InterruptedException, IOException
		{
			colValMap.clear();
			Iterator<Entry<String, Object>> iterator = labelValMap.entrySet().iterator();
			while(iterator.hasNext())
			{
				Entry<String, Object> entry = iterator.next();
				String label=entry.getKey();
				String val=entry.getValue().toString();
				if(labelColMap.containsKey(label))
				{
					String col=labelColMap.get(label);
					colValMap.put(col, val);
				}
			}
		}
		
		public void extraProc() throws InterruptedException, IOException{}
		
		public void updateToDB() throws SQLException
		{
			StringBuilder colBuilder=new StringBuilder();
			StringBuilder valBuilder=new StringBuilder();
			if(colValMap.containsKey("RegistrationNo"))
			{
				colBuilder.append("lastUpdateTime");
				valBuilder.append("getDate()");
			}
			else
			{
				colBuilder.append("RegistrationNo,lastUpdateTime");
				valBuilder.append(String.format("'%s',getDate()", curCode));
			}
			Iterator<Entry<String, String>> iterator = colValMap.entrySet().iterator();
			while(iterator.hasNext())
			{
				Entry<String, String> entry = iterator.next();
				String col=entry.getKey();
				String val=entry.getValue();
				colBuilder.append(","+col);
				valBuilder.append(",'"+val+"'");
			}
			
			String insertCmd=String.format("insert into %s(%s) values(%s)", tableName,colBuilder,valBuilder);
			dbClient.addBatch(insertCmd);
//			System.out.println(insertCmd);
		}
	}
	
	class JiBenXinXi extends HtmlTable
	{

		public JiBenXinXi(String tableDesc, String tableName) {
			super(tableDesc, tableName);
			// TODO Auto-generated constructor stub
		}
		
		public void parseTable(WebElement tableDiv) throws SQLException
		{
			colValMap.clear();
			List<WebElement> rows = waitForWebElements(tableDiv,By.xpath("div/table/tbody/tr"));
//			System.out.println(driver.getPageSource());

			for(WebElement row:rows)
			{
				List<WebElement> cols = row.findElements(By.xpath("th"));
				List<WebElement> vals = row.findElements(By.xpath("td"));
				if(cols.size()==vals.size())
				{
					for(int i=0;i<cols.size();i++)
					{
						String desc=cols.get(i).getText().trim();
						if(!desc.equals(""))
						{
							String col = descColMap.get(desc);
							String val = vals.get(i).getText().trim();
							if(!val.equals(""))
							{
								if(col==null)
								{
									throw new IllegalArgumentException("������Ϣ����δ֪!code:"+curCode+",colDesc:"+desc);
								}
								colValMap.put(col, val);
							}
						}
					}
				}
			}
			colValMap.put("Province", province);
			updateToDB();
		}
	}

	class QingSuanXinXi extends HtmlTable
	{

		public QingSuanXinXi(String tableDesc, String tableName) {
			super(tableDesc, tableName);
			// TODO Auto-generated constructor stub
		}
		public void parseTable(WebElement tableDiv) throws InterruptedException, SQLException, IOException
		{
			try
			{
				String divText=tableDiv.getText();
				String personInCharge=divText.substring(divText.indexOf("������Ϣ���㸺����")+"������Ϣ���㸺����".length()).trim();
				
				
				String otherArgsStr=jsCode.substring(jsCode.indexOf("],")+2);
				String[] otherArgsArr=StringUtils.split(otherArgsStr,",");
				funcArgs=new String[otherArgsArr.length+1];
				funcArgs[0]=jsCode.substring(jsCode.indexOf("[")+1,jsCode.indexOf("]")).trim();
				for(int i=0;i<otherArgsArr.length;i++)
				{
					funcArgs[i+1]=otherArgsArr[i].substring(otherArgsArr[i].indexOf("\"")+1,otherArgsArr[i].lastIndexOf("\""));
				}

				int startIdx=0,stopIdx=-1;
				while(stopIdx!=(funcArgs[0].length()-1))
				{
					startIdx=funcArgs[0].indexOf("{", stopIdx);
					stopIdx=funcArgs[0].indexOf("}", startIdx);
					String jsonStr=funcArgs[0].substring(startIdx, stopIdx+1);
					labelValMap=JSON.parseObject(jsonStr);
					labelValMap.put("personInCharge", personInCharge);
					
					updateColValMap();
					extraProc();
					updateToDB();
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				return;
			}
		}
	}
	
	class GuDongXinXi extends HtmlTable
	{
		String[] subColumnArr=new String[]{
				"Subscripted_Capital","Subscripted_Method","Subscripted_Amount","Subscripted_Time",
				"ActualPaid_Capital","ActualPaid_Method","ActualPaid_Amount","ActualPaid_Time"};
		public GuDongXinXi(String tableDesc, String tableName) {
			super(tableDesc, tableName);
			// TODO Auto-generated constructor stub
		}
		public void extraProc() throws InterruptedException, IOException
		{
			
			String flag=funcArgs[funcArgs.length-1];
			colValMap.put("detailFlag", flag);
			if(flag.equals("true"))
			{
				String invid=labelValMap.getString("invid");
				((JavascriptExecutor)driver).executeScript("window.open('" + labelValMap.getString("detailUrl") + "')");
				String pripid=funcArgs[funcArgs.length+Integer.valueOf(pripidIdx)];
				switchToPage(detailShareholderInfoPageTitle);			
				String jsCode="$(document).ready(function(){ "
						+"var webAppName =\"/saicpub\"; "
						+"var pripid =\""+pripid+"\"; "
						+"var invid =\""+invid+"\"; "
						+"setParamTzrxx(webAppName,pripid,invid); "
						+"getTzrxxList(webAppName); "
						+"}); ";
				((JavascriptExecutor)driver).executeScript(jsCode);
				try
				{
					waitForWebElement(By.xpath("/html/body/div[2]/div/div/table/tbody/tr[4]/td[not(text()='')]"),5);
					List<WebElement> subVals = waitForWebElements(By.xpath("/html/body/div[2]/div/div/table/tbody/tr[4]/td[not(contains(@style,'display:none'))]"));
					for(int j=0;j<8;j++)
					{
						String col=subColumnArr[j];
						String val=subVals.get(j+1).getText();
						colValMap.put(col, val);
					}
				}
				catch (TimeoutException e)
				{
					logger.info("������Ϣ�����ڡ�");
				}
				driver.close();
				switchToPage(detailPageTitle);	
			}
		}
	}
	
	class DongChanDiYaDengJiXinXi extends HtmlTable
	{
		public DongChanDiYaDengJiXinXi(String tableDesc, String tableName) {
			super(tableDesc, tableName);
			// TODO Auto-generated constructor stub
			
		}
		public void extraProc()
		{
			if(colValMap.containsKey("ChattelMortgage_GuaranteedAmount") 
					&& !colValMap.get("ChattelMortgage_GuaranteedAmount").equals(""))
			{
				colValMap.put("ChattelMortgage_GuaranteedAmount", colValMap.get("ChattelMortgage_GuaranteedAmount")+"��Ԫ");
			}
		}
	}
	
	class GuQuanChuZhiDengJiXinXi extends HtmlTable
	{
		public GuQuanChuZhiDengJiXinXi(String tableDesc, String tableName) {
			super(tableDesc, tableName);
			// TODO Auto-generated constructor stub
		}
		
		public void extraProc()
		{
			if(colValMap.containsKey("EquityPledge_Amount_Prefix") 
					&& !colValMap.get("EquityPledge_Amount_Prefix").equals("")
					&& colValMap.containsKey("EquityPledge_Amount_Suffix")
					&& !colValMap.get("EquityPledge_Amount_Suffix").equals(""))
			{
				String equityPledgeAmount=colValMap.get("EquityPledge_Amount_Prefix")
						+colValMap.get("EquityPledge_Amount_Suffix").equals("");
				colValMap.remove("EquityPledge_Amount_Prefix");
				colValMap.remove("EquityPledge_Amount_Suffix");
				colValMap.put("EquityPledge_Amount",equityPledgeAmount);
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
		LnGsSearcherHandless searcher= new LnGsSearcherHandless();
		searcher.setDbClient(client);
		searcher.setLogger(new Logger("test"));
		searcher.initDriver();
		
		String[] codeArr=new String[]{"210106100034253","210102000101329","210102000002031","210000004951110","210100000027184","210102100027500","210102100027989","210200000285085","210200000287380","210000004943780","210000004931543","210000004951747","210100400011413","210241000144367","210513000001412","210241000141846","210123000004123","211221004036687","210123000021460","210000004918831","210106000058876","210123000017434","210123000028277","210123000024675"};
		for(String code:codeArr)
		{
			GsModel model = searcher.search(code);
			String updateCmd="update GsSrc set "+model+" where registeredCode='"+code+"'";
			System.out.println(new Date());
			System.out.println(updateCmd);
			break;
		}
		searcher.dbClient.executeBatch();
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