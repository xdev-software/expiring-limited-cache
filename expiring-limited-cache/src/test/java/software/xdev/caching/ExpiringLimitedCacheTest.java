/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
