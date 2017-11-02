package tools;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
public class SysConfig {

	public static String workDir=System.getProperty("user.dir");
		
//	public static String MSSQL_HOST="localhost"; //主机
//	public static String MSSQL_USER="sa"; //用户名
//	public static String MSSQL_PWD="likai123"; //密码
//	public static String MSSQL_DB="pachong"; //数据库
	
	public static String CERTIFICATE_SAVE_PATH=workDir+"\\data\\certificate\\"; //机构证书截图保存路径
	public static String VALIDATE_CODE_SAVE_PATH=workDir+"\\data\\validate\\"; //验证码截图保存路径
	public static String LOG_FILE_SAVE_PATH=workDir+"\\logs\\"; //日志文件保存路径
	
	public static String FIREFOX_PROFILE=workDir+"\\conf\\FirefoxProfile";
	
	public static String BDYY_OCR=workDir+"\\yzm\\bdyy\\bdyy.bat"; //百度应用验证码识别程序
	public static String ZZJGDM_OCR=workDir+"\\yzm\\zzjgdm\\zzjgdm.bat"; //官网验证码识别程序
	public static String LIAONING_OCR=workDir+"\\yzm\\liaoning\\liaoning.bat"; //辽宁识别程序
//	
	public static String MSSQL_HOST="114.215.140.37"; //主机
	public static String MSSQL_USER="likai"; //用户名
	public static String MSSQL_PWD="$qhz,gS*Z6S#3N\"*"; //密码
	public static String MSSQL_DB="pachong"; //数据库

	public static int MAX_TRY_TIMES=5;//Integer.MAX_VALUE;
	public static int WAIT_IN_SECONDS=10;
//	public static int SLEEP_IN_MILLIS=500; //default value is 500
	
	public static SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static String getCertificateSavePath(String orgCode)
	{
		String today=sdf.format(new Date()).substring(0,10);
		String savePath=CERTIFICATE_SAVE_PATH+today;
		if(!new File(savePath).exists())
		{
			new File(savePath).mkdirs();
		}
		
		return savePath+"\\certificate_"+orgCode+".png";
	}
	
	public static String getValidateCodeSavePath(String orgCode)
	{
		return VALIDATE_CODE_SAVE_PATH+"validateCode_"+orgCode+".png";
	}
	
	public static String getError(Exception e)
	{
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw, true));
		return sw.toString();
	}
	
	public static String getCurTime()
	{
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
	}
	
	public static String getLocalHost() throws IOException
	{
		String localhost=ProcessInfo.getHostName();
		File file=new File("D:\\PublishAgent\\LocalIp.txt");
		if(file.exists())
		{
			InputStreamReader reader = new InputStreamReader(new FileInputStream(file),"gbk");
		    BufferedReader bufferedReader = new BufferedReader(reader);
		    localhost=bufferedReader.readLine().trim();
		    bufferedReader.close();
		    reader.close();
		}
	    return localhost;
	}
	
	public static void main(String[] args) throws IOException
	{
		System.out.println(getLocalHost());
	}

}
