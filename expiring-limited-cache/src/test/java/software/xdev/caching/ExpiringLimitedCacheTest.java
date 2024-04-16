package software.xdev.caching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


class ExpiringLimitedCacheTest
{
	@Test
	void checkLimit()
	{
		final ExpiringLimitedCache<Integer, String> cache =
			new ExpiringLimitedCache<>("limit", Duration.ofMinutes(1), 1);
		
		final String value1 = "1";
		final String value2 = "2";
		
		cache.put(1, value1);
		assertEquals(value1, cache.get(1));
		
		cache.put(2, value2);
		assertNull(cache.get(1));
		assertEquals(value2, cache.get(2));
	}
	
	@SuppressWarnings("java:S2925") // We are handling time
	@Test
	@EnabledIfSystemProperty(named = "run.time.based", matches = ".*")
	void checkExpiration()
	{
		final Duration expirationTime = Duration.ofSeconds(1);
		final ExpiringLimitedCache<Integer, String> cache =
			new ExpiringLimitedCache<>("expiration", expirationTime, 100);
		
		final String value1 = "1";
		cache.put(1, value1);
		try
		{
			Thread.sleep(expirationTime.toMillis() / 4);
		}
		catch(final InterruptedException iex)
		{
			Thread.currentThread().interrupt();
		}
		assertEquals(value1, cache.get(1));
		
		cache.put(1, value1);
		try
		{
			Thread.sleep(expirationTime.toMillis() * 2);
		}
		catch(final InterruptedException iex)
		{
			Thread.currentThread().interrupt();
		}
		assertNull(cache.get(1));
	}
}
