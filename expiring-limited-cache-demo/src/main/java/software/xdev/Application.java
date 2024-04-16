package software.xdev;

import java.time.Duration;

import software.xdev.caching.ExpiringLimitedCache;


@SuppressWarnings("java:S106")
public final class Application
{
	private static final ExpiringLimitedCache<Integer, String> CACHE = new ExpiringLimitedCache<>(
		"demo",
		Duration.ofSeconds(2),
		1
	);
	
	public static void main(final String[] args)
	{
		CACHE.put(1, "HI");
		CACHE.put(2, "DEMO");
		// Returns null as size > max
		System.out.println("1=" + CACHE.get(1));
		System.out.println("2=" + CACHE.get(2));
		
		System.out.println("Waiting a moment...");
		try
		{
			Thread.sleep(5 * 1000L);
		}
		catch(final InterruptedException iex)
		{
			Thread.currentThread().interrupt();
		}
		
		// Will also return null as expired
		System.out.println("2=" + CACHE.get(2));
	}
	
	private Application()
	{
	}
}
