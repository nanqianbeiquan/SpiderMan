package tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	
	public FileWriter fw;
	public SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss -> ");
		
	public void info(String s) throws IOException
	{
		String infoStr=df.format(new Date())+s+"\r\n";
		System.out.print(infoStr);
		fw.write(infoStr);
		
		flush();
	}
	
	public void close() throws IOException
	{
		fw.close();
	}
	
	public Logger(String logName) throws IOException
	{
		File logFile=new File(SysConfig.LOG_FILE_SAVE_PATH+logName+".txt");
		fw=new FileWriter(logFile,true);
		
	}
	
	public void flush() throws IOException
	{
		fw.flush();
	}
	
	public static  void main(String[] args) throws IOException
	{
		Logger logger=new Logger("test");
		logger.info("1\n23\n\r4");
		logger.close();
	}
	
}
