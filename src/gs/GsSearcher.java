package gs;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxProfile;

import tools.MSSQLClient;
import tools.SearcherFirefox;

public abstract class GsSearcher extends SearcherFirefox {

	public String searcherUrl=null;
	public String validateUrl=null;
	public String curCode;
	public FirefoxProfile profile = new FirefoxProfile();
	public WebElement codeInput;
	public By codeInputXpath;
	public WebElement validateImage;
	public By validateImageXpath;
	public WebElement validateInput;
	public By validateInputXpath;
	public WebElement listContent;
	public By listContentXpath;
	public WebElement openDeatil;
	public By openDetailXpath;
	public WebElement abstractInfo;
	public By abstractInfoXpath;
	public HashMap<String,HtmlTable> tableMap=new HashMap<String,HtmlTable>();
	public MSSQLClient dbClient;
	
	public void setDbClient(MSSQLClient dbClient)
	{
		this.dbClient=dbClient;
	}
	
	public abstract void switchToSearchPage() throws Exception;
	public abstract void switchToDetailPage() throws Exception;
	/*
	 * ����ʵ��
	 */
//	public void loadHtmlTableConfig(String configPath) throws JDOMException, IOException
//	{
//		SAXBuilder builder = new SAXBuilder();
//		Document doc = builder.build(new File(configPath));
//		Element rootEle = doc.getRootElement();
//		List<Element> tableList = rootEle.getChildren("table");
//		for(Element table:tableList)
//		{
//			String tableDesc=table.getChildText("desc");
//			String tableName=table.getChildText("name");
//			String by=table.getChildText("by");
//			String pripidIdx=table.getChildText("pripidIdx");
//			String detailUrl=table.getChildText("detailUrl");
//			
//			if(tableDesc.equals("������Ϣ"))
//			{
//				tableMap.put(tableDesc, new JiBenXinXi(tableDesc, tableName));
//			}
//			else if (tableDesc.equals("�ɶ���Ϣ"))
//			{
//				tableMap.put(tableDesc, new GuDongXinXi(tableDesc, tableName));
//			}
//			else if (tableDesc.equals("������Ϣ"))
//			{
//				tableMap.put(tableDesc, new QingSuanXinXi(tableDesc, tableName));
//			}
//			else if (tableDesc.equals("������Ѻ�Ǽ���Ϣ"))
//			{
//				tableMap.put(tableDesc,new DongChanDiYaDengJiXinXi(tableDesc, tableName));
//			}
//			else if(tableDesc.equals("��Ȩ���ʵǼ���Ϣ"))
//			{
//				tableMap.put(tableDesc,new GuQuanChuZhiDengJiXinXi(tableDesc, tableName));
//			}
//			else
//			{
//				tableMap.put(tableDesc,new HtmlTable(tableDesc,tableName));
//			}
//			
//			tableMap.get(tableDesc).identity=Boolean.valueOf(table.getChildText("identity"));
//			tableMap.get(tableDesc).pripidIdx=pripidIdx;
//			tableMap.get(tableDesc).detailUrlTemplate=detailUrl;
//			List<Element> columns = table.getChildren("column");
//
//			if(by.equals("desc"))
//			{
//				tableMap.get(tableDesc).descColMap=new HashMap<String,String>();
//				for(Element column:columns)
//				{
//					String columnDesc = column.getAttributeValue("desc");
//					String columnName = column.getText();
//					tableMap.get(tableDesc).descColMap.put(columnDesc,columnName);
//				}
//			}
//			else if(by.equals("label"))
//			{
//				tableMap.get(tableDesc).labelColMap=new HashMap<String,String>();
//				for(Element column:columns)
//				{
//					String columnLabel = column.getAttributeValue("label");
//					String columnName = column.getText();
//					tableMap.get(tableDesc).labelColMap.put(columnLabel, columnName);
//				}
//			}
//		}
//	}
	
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
		
		public HtmlTable(String tableDesc,String tableName)
		{
			this.tableDesc=tableDesc;
			this.tableName=tableName;
		}
		
		public void setJs(String js)
		{
			this.js=js;
		}
		//ɾ��ԭ�����ݼ�¼
		public void deleteFromDB() throws SQLException
		{
			String deleteCmd=String.format("delete from %s where RegistrationNo='%s'", tableName,curCode);
			dbClient.addBatch(deleteCmd);
		}
		//�������ݣ�����������������ݿ�
		public void parseTable(WebElement tableDiv) throws InterruptedException, SQLException, IOException
		{
			
		}
		
	}

}