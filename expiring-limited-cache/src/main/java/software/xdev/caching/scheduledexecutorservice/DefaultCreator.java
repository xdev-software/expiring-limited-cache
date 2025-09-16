package software.xdev.caching.scheduledexecutorservice;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;


public final class DefaultCreator
{
	public static ScheduledExecutorService create()
	{
		final AtomicInteger counter = new AtomicInteger(0);
		return Executors.newScheduledThreadPool(
			1, r -> {
				final Thread thread = new Thread(r);
				thread.setName("Cache-Cleanup-Executor-" + counter.getAndIncrement());
				thread.setDaemon(true);
				return thread;
			});
	}
	
	private DefaultCreator()
	{
	}
}
