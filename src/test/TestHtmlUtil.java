package test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.alibaba.fastjson.JSONArray;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class TestHtmlUtil {
	
	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		
		WebDriver driver=new FirefoxDriver();
		driver.get("http://search.51job.com/jobsearch/search_result.php?fromJs=1&jobarea=020000&funtype=0000&industrytype=00&keywordtype=2&lang=c&stype=2&postchannel=0000&fromType=1&confirmdate=9");
		
		WebElement myElement=new WebDriverWait(driver, 10).until(new ExpectedCondition() {
			public Object apply(Object arg0) {
				WebDriver d=(WebDriver)arg0;
				return d.findElement(By.className("el"));
			}
		});
		
		List<WebElement> list=driver.findElements(By.xpath(".//*[@id='resultList']/div[*]/span[1]/a")); 
		for(WebElement e:list){
			System.out.println(e.getText());
		}
//		WebElement nextpage=driver.findElement(By.xpath("//html/body/div[2]/div[5]/div/div/div/ul/li[8]/*"));
		boolean flag=true;
		while(flag){
			WebElement nextpage=driver.findElement(By.xpath("//html/body/div[2]/div[5]/div/div/div/ul/li[8]/*"));
			if(nextpage.getTagName().equals("a")){
				nextpage.click();
				@SuppressWarnings("unchecked")
				WebElement myElement2=new WebDriverWait(driver, 10).until(new ExpectedCondition() {
					public Object apply(Object arg0) {
						WebDriver d=(WebDriver)arg0;
						return d.findElement(By.className("el"));
					}
					
				});
				List<WebElement> list2=driver.findElements(By.xpath(".//*[@id='resultList']/div[*]/span[1]/a")); 
				for(WebElement ee:list2){
					System.out.println(ee.getText());
				}
				//nextpage=driver.findElement(By.xpath("//html/body/div[2]/div[5]/div/div/div/ul/li[8]/a"));
				continue;
			}else{
				flag=false;
			}
		}
	}
}
