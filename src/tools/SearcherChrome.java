package tools;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class SearcherChrome extends Searcher{

	public String fireFoxPath=null;
	public int validateOffsetX,validateOffsetY;
	public WebElement validateImage;
	
	public Runtime runtime=Runtime.getRuntime();
	
	public SearcherChrome()
	{
		System.setProperty("webdriver.chrome.driver", SysConfig.workDir+"\\plugins\\chromedriver.exe");
	}

	public int initDriver() throws Exception
	{
		int res=1;
		logger.info("Initializing driver...");
		driver=new ChromeDriver();
		logger.info("Driver initialized succeed!");
//		driver.manage().window().maximize();
		driver.manage().timeouts().pageLoadTimeout(SysConfig.WAIT_IN_SECONDS, TimeUnit.SECONDS);
		driver.manage().timeouts().implicitlyWait(SysConfig.WAIT_IN_SECONDS, TimeUnit.SECONDS);
		driver.manage().timeouts().setScriptTimeout(SysConfig.WAIT_IN_SECONDS, TimeUnit.SECONDS);
		IpForbidden=false;
		for(int i=0;i<SysConfig.MAX_TRY_TIMES;i++)
		{
			if(loadStartPage()==true)
			{
				res=0;
				break;
			}
			else
			{
				logger.info("load start Page failed!");
			}
		}
		searchWindowHandle=driver.getWindowHandle();
		return res;
	}

	//截图获取验证码
	public void screenShot(String savePath,WebElement imageEle,int offsetX,int offsetY) throws IOException
	{
		Point imgLocation = imageEle.getLocation();
		Dimension imgSize = imageEle.getSize();
		
		WebDriver augmentedDriver = new Augmenter().augment(driver);
		byte[] takeScreenshot = ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.BYTES);
		BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(takeScreenshot));
		BufferedImage croppedImage = originalImage.getSubimage(
				offsetX+imgLocation.getX(),
				offsetY+imgLocation.getY(),
				imgSize.getWidth(),
				imgSize.getHeight());
		ImageIO.write(croppedImage, "png", new File(savePath));
	}
	
	public String recongnizeValidateCode(String pluginPath,String imagePath) throws IOException
	{
		String validateCode=null;
		String cmd="cmd /c "+pluginPath+" "+imagePath;
		try
		{
			Process process = runtime.exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line=null;
			int i=0;
			while((line=reader.readLine())!=null)
			{
				if((++i)==7)
				{
					validateCode=line.trim();
					reader.close();
					break;
				}
			}
		}
		catch (Exception e)
		{
			logger.info("Recongnize validate code failed.Try again...");
			logger.info(SysConfig.getError(e));
		}
		new File(imagePath).delete();
		return validateCode;
	}
	
	public int quitDriver() throws IOException
	{
		logger.info("Driver is quitting...");
		try
		{
			driver.quit();
		}
		catch (RuntimeException e)
		{
			logger.info(SysConfig.getError(e));
		}
		logger.info("Driver quitting succeed!");
		return 0;
	}
}
