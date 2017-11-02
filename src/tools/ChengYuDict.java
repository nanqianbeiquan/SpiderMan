package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

public class ChengYuDict {

	public HashSet<String> dictionary=new HashSet<String>();
	HashSet<String> tmpSet=new HashSet<String>();
	
	public String getWord(String wordStr)
	{
		String[] wordArr=wordStr.split("|");
//		System.out.println(Arrays.asList(wordArr));
		for(int i0=0;i0<wordArr.length;i0++)
		{
			for(int i1=0;i1<wordArr.length;i1++)
			{
				while(i1==i0)
				{
					i1++;
				}
				if(i1<wordArr.length)
				{
					for(int i2=0;i2<wordArr.length;i2++)
					{
						while(i2==i0 || i2==i1) 
						{
							i2++;
						}
						if(i2<wordArr.length)
						{
							for(int i3=0;i3<wordArr.length;i3++)
							{
								while(i3==i0 || i3==i1 || i3==i2)
								{
									i3++;
								}
								if(i3<wordArr.length)
								{
									String word=wordArr[i0]+wordArr[i1]+wordArr[i2]+wordArr[i3];
									if(dictionary.contains(word))
									{
										tmpSet.add(word);
									}
								}
							}
						}
					}
				}
			}
		}
		String res=StringUtils.join(tmpSet, ",");
		tmpSet.clear();
//		System.out.println(res);
		return res;
	}
	public void loadDict(String dictPath) throws IOException
	{
		File file=new File(dictPath);
		
		InputStreamReader reader = new InputStreamReader(new FileInputStream(file),"gbk");
	    BufferedReader bufferedReader = new BufferedReader(reader);
	    String lineText = null;
	    while((lineText = bufferedReader.readLine()) != null)
	    {
	    	String[] words = lineText.split(",");
	    	for(String word:words)
	    	{
	    		if(word.length()<=4)
	    		{
	    			dictionary.add(word);
	    		}
	    	}
	    }
	    bufferedReader.close();
            
	}
	
	public static void main(String[] args) throws IOException
	{
		ChengYuDict dict=new ChengYuDict();
		dict.loadDict("F:\\EclipseProjects\\SpiderMan\\成语字典.txt");
		dict.getWord("之万美光成军人马千");
//		dict.getWord(new String[]{"恳","勤","雨","勤","恳","风","细","斜","风"});
//		dict.getWord(new String[]{"之","万","美","光","成","军","人","马","千"});
//		dict.transfer();
	}
}
