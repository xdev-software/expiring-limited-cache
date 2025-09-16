package software.xdev.caching.scheduledexecutorservice;

import java.util.concurrent.ScheduledExecutorService;


public final class DefaultHolder
{
	private static ScheduledExecutorService instance;
	
	public static ScheduledExecutorService instance()
	{
		if(instance == null)
		{
			init();
		}
		return instance;
	}
	
	private static synchronized void init()
	{
		if(instance != null)
		{
			return;
		}
		instance = DefaultCreator.create();
	}
	
	private DefaultHolder()
	{
	}
}
