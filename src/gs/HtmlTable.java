package gs;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.openqa.selenium.WebElement;

import com.alibaba.fastjson.JSONObject;

import tools.MSSQLClient;

public class HtmlTable {

	public String tableName;
	public String tableDesc;
	public HashMap<String,String> descColMap; //解析基本信息
	public String[] ColArr; //解析剩余信息
	public HashMap<String,String> colValMap=new HashMap<String,String>();
	public boolean identity;
	public MSSQLClient dbClient;
	
	public HtmlTable(String tableDesc,String tableName)
	{
		this.tableDesc=tableDesc;
		this.tableName=tableName;
	}
	
	public void setDbClient(MSSQLClient client)
	{
		this.dbClient=client;
	}
	
	public void deleteFromDB(String code) throws SQLException
	{
		String deleteCmd=String.format("delete from %s where RegistrationNo='%s'", tableName,code);
		dbClient.addBatch(deleteCmd);
	}
	
	public void parseTable(WebElement tableDiv) throws InterruptedException, SQLException, IOException
	{
		
	}
	
	public void updateToDB(String code) throws SQLException
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
			valBuilder.append(String.format("'%s',getDate()", code));
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
//		System.out.println(insertCmd);
	}
}
