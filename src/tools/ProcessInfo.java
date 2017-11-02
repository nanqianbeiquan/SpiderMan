package tools;

import java.lang.management.ManagementFactory;

public class ProcessInfo {

	static String runtimeMXBeanName = ManagementFactory.getRuntimeMXBean().getName();
	public static String getPID()
	{
		return runtimeMXBeanName.split("@")[0];
	}
	
	public static String getHostName()
	{	
		return runtimeMXBeanName.split("@")[1];
	}
}
