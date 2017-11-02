package tools;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebWindow;

public class ExtendedHtmlUnitDriver extends HtmlUnitDriver{
	
	public ExtendedHtmlUnitDriver()
	{
		super();
		configSecurity();
	}
	
	public ExtendedHtmlUnitDriver(boolean enableJavascript)
	{
		super(enableJavascript);
		configSecurity();
	}
	public ExtendedHtmlUnitDriver(BrowserVersion version,boolean enableJavascript)
	{
		super(version,enableJavascript);
		configSecurity();
	}
	
	public void configSecurity()
	{
		java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF); 
		java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
		System.setProperty("jsse.enableSNIExtension", "false");
		getWebClient().getOptions().setUseInsecureSSL(true);
//		getWebClient().setAjaxController(new NicelyResynchronizingAjaxController());//很重要，设置支持AJAX
	}
	
	public void downloadImage(String url,String savePath) throws FailingHttpStatusCodeException, MalformedURLException, IOException, InterruptedException
	{
		String curWindow=getWindowHandle();
		URL imageURL=new URL(url);
		WebWindow yzmWindow= getWebClient().openWindow(imageURL, url+"_snapshot");
//		WebWindow window = getWebClient().openWindow(imageURL, url+"_snapshot");
		UnexpectedPage page = getWebClient().getPage(yzmWindow,new WebRequest(imageURL));
		byte[] imageBytes = IOUtils.toByteArray(page.getWebResponse().getContentAsStream());
		BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
		ImageIO.write(originalImage, "png", new File(savePath));
		switchTo().window(url+"_snapshot");
		close();
		switchTo().window(curWindow);
	}
	
	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException
	{
		ExtendedHtmlUnitDriver driver=new ExtendedHtmlUnitDriver(true);
		driver.get("http://www.court.gov.cn/zgcpwsw/");
		driver.get("http://www.court.gov.cn/zgcpwsw/List/List?sorttype=1&conditions=searchWord+1+AJLX++%E6%A1%88%E4%BB%B6%E7%B1%BB%E5%9E%8B:%E5%88%91%E4%BA%8B%E6%A1%88%E4%BB%B6");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(driver.getPageSource());
//		driver.downloadValidateImage("https://s.nacao.org.cn/servlet/ValidateCodeServlet", "D:\\validate\\validate.png");
		
	}
}
