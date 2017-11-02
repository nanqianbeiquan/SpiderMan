package tools;

import java.util.HashMap;

public class JobConfig
{
	public String jobName;
	public HashMap<String,String> confArgs=new HashMap<String,String>();
	
	public JobConfig(String[] args)
	{
		for(int i=0;i<args.length;i++)
		{
			String arg=args[i];
			if(arg.startsWith("--jobName="))
			{
				jobName=arg.substring("--jobName=".length());
			}
			else
			{
				int idx=arg.indexOf("=");
				String key=arg.substring(2,idx);
				String val=arg.substring(idx+1);
				confArgs.put(key,val);
			}
		}
	}
	
	public String getString(String propertyName)
	{
		return confArgs.get(propertyName);
	}
	
	public Integer getInteger(String propertyName)
	{
		return Integer.valueOf(confArgs.get(propertyName));
	}
	
	public Boolean getBoolean(String propertyName)
	{
		return Boolean.valueOf(confArgs.get(propertyName));
	}
	
	public boolean hasProperty(String propertyName)
	{
		return confArgs.containsKey(propertyName);
	}
	
	public String toString()
	{
		return jobName+" -> "+confArgs;
	}
}