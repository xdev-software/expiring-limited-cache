package software.xdev;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.caching.ExpiringLimitedCache;


public final class Application
{
	private static final Logger LOG = LoggerFactory.getLogger(Application.class);
	
	private static final ExpiringLimitedCache<Integer, String> CACHE = new ExpiringLimitedCache<>(
		"demo",
		Duration.ofSeconds(2),
		1
	);
	
	@SuppressWarnings("java:S2629") // No we don't write if everywhere now
	public static void main(final String[] args)
	{
		CACHE.put(1, "HI");
		CACHE.put(2, "DEMO");
		// Returns null as size > max
		LOG.info("1={}", CACHE.get(1));
		LOG.info("2={}", CACHE.get(2));
		
		LOG.info("Waiting a moment...");
		try
		{
			Thread.sleep(3 * 1000L);
		}
		catch(final InterruptedException iex)
		{
			Thread.currentThread().interrupt();
		}
		
		// Will also return null as expired
		LOG.info("2={}", CACHE.get(2));
	}
	
	private Application()
	{
	}
}
