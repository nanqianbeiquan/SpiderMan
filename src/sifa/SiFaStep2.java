package sifa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.HttpStatusException;

import tools.Logger;
import tools.MSSQLClient;
import tools.ProcessInfo;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class SiFaStep2{

	public MSSQLClient dbClient;
	public String saveDir="D:/sifa/new/";
	public File parentDir;
	public int batchSize=30;
	public ArrayList<String> pageList=new ArrayList<String>();
	public Logger logger;
	
	WebClient webClient = new WebClient(BrowserVersion.CHROME);
	
	public SiFaStep2() throws IOException, ClassNotFoundException, SQLException {
		
		java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF); 
		java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
		
		dbClient=new MSSQLClient(
				String.format("jdbc:sqlserver://121.42.41.188:1433;DatabaseName=judgment"),
				"likai", //user
				"2$sQNOQTb%", //pwd
				false //autoCommit
				);
		String today=new SimpleDateFormat("yyyyMMdd").format(new Date());
		logger=new Logger("SiFa_"+today+"_"+ProcessInfo.getPID());
		
		parentDir=new File(saveDir+today);
		if(!parentDir.exists())
		{
			parentDir.mkdir();
		}
	}

	public String getDoc(String URL) throws IOException
	{
//		Connection conn = Jsoup.connect(URL);
//		conn.userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.64 Safari/537.31").timeout(10000);
//		return conn.get();
		HtmlPage page = webClient.getPage(URL);
		return page.asText();
	}
	
	public int savePage(String URL) throws IOException
	{
		try{
			String fileName=URL.substring(URL.indexOf("DocID=")+6);
			String savePath=parentDir+"/"+fileName+".txt";
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(savePath),"UTF-8");
			out.write(getDoc(URL));
			out.close();
			return 1;
		}
		catch (HttpStatusException e)
		{
//			System.out.println(e.getMessage());
			return e.getStatusCode();
		}
		catch (FailingHttpStatusCodeException e)
		{
//			System.out.println(e);
			return e.getStatusCode();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 0;
		}	
	}
	
	public void execUpdateProc() throws Exception {

		int totalUpdateCnt=0;
		logger.info("totalUpdateCnt:"+totalUpdateCnt);
		while(true)
		{
			String sql0=String.format("select top %d filePage from DocumentPage(tablockx) where updateStatus=0", batchSize);
			dbClient.execute(sql0);
			ResultSet res0 = dbClient.execute(sql0);
			StringBuilder inStr=new StringBuilder("");
			while(res0.next()==true)
			{
				String filePage=res0.getString(1);
				pageList.add(filePage);
				inStr.append("'"+filePage+"',");
//				dbClient.statement.addBatch("update DocumentPage set updateStatus=1 where filePage='"+filePage+"'");
			}
			if(inStr.length()>0)
			{
				inStr.setLength(inStr.length()-1);
			}
			
			String sql1=String.format("update DocumentPage set updateStatus=-1 where filePage in (%s)",inStr);
			if(pageList.isEmpty())
			{
				break;
			}
			dbClient.statement.execute(sql1);
			res0.close();
//			dbClient.statement.executeBatch();
			dbClient.commit();
//			dbClient.statement.clearBatch();
			for(String filePage:pageList)
			{
				int saveRes=0;
				for(int i=0;i<5;i++)
				{
					saveRes=savePage(filePage);
					if(saveRes==1)
					{
						break;
					}
				}
//				System.out.println("update DocumentPage set updateStatus="+saveRes+" where filePage='"+filePage+"'");
//				String fileName=filePage.substring(filePage.indexOf("DocID=")+6);
				dbClient.addBatch("update DocumentPage set updateStatus="+saveRes+" where filePage='"+filePage+"'");
//				dbClient.statement.addBatch("insert into DocumentPageUpdateStatus(filePage,updateStatus) values('"+filePage+"',"+saveRes+")");
			}
			dbClient.executeBatch();
			dbClient.clearBatch();
			dbClient.commit();
			
			totalUpdateCnt+=pageList.size();
			logger.info("totalUpdateCnt:"+totalUpdateCnt);
			pageList.clear();
		}
		logger.info("Mession complete!");
		logger.close();
	}

	public static String delHTMLTag(String htmlStr) {
		String regEx_script = "<script[^>]*?>[\\s\\S]*?<\\/script>"; // 定义script的正则表达式
		String regEx_style = "<style[^>]*?>[\\s\\S]*?<\\/style>"; // 定义style的正则表达式
		String regEx_html = "<[^>]+>"; // 定义HTML标签的正则表达式

		Pattern p_script = Pattern.compile(regEx_script,
				Pattern.CASE_INSENSITIVE);
		Matcher m_script = p_script.matcher(htmlStr);
		htmlStr = m_script.replaceAll(""); // 过滤script标签

		Pattern p_style = Pattern
				.compile(regEx_style, Pattern.CASE_INSENSITIVE);
		Matcher m_style = p_style.matcher(htmlStr);
		htmlStr = m_style.replaceAll(""); // 过滤style标签

		Pattern p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
		Matcher m_html = p_html.matcher(htmlStr);
		htmlStr = m_html.replaceAll(""); // 过滤html标签

		return htmlStr.trim(); // 返回文本字符串
	}

	
	public static void main(String[] args) throws Exception
	{
		SiFaStep2 job=new SiFaStep2();
		job.savePage("http://www.court.gov.cn/zgcpwsw/content/content?DocID=9039823b-33d4-404e-b287-996e130c5d86");
//		job.execUpdateProc();
//		String URL="http://www.court.gov.cn/cpwsw/sx/sxsxzszjrmfy/ms/201411/t20141111_4042340.htm";
//		System.out.println(job.getDoc("http://www.court.gov.cn/zgcpwsw/content/content?DocID=00054667-de86-43fa-8c83-138427e11907").html());
//		String[] tmpArr=StringUtils.split(URL,"/");
//		File parentDir=new File(job.saveDir+tmpArr[tmpArr.length-2]);
//		String savePath=job.saveDir+tmpArr[tmpArr.length-2]+"/"+tmpArr[tmpArr.length-1]+".txt";
//		System.out.println(savePath);
	}
}
