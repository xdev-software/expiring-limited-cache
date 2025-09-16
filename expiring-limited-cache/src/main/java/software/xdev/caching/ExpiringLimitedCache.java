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

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.caching.scheduledexecutorservice.DefaultHolder;


/**
 * Caches values until
 * <ul>
 *     <li>a specific time (>=1s) elapses (is cleared in batches by a timer so it may stick around a bit longer)
 *     - {@link #expirationTime}</li>
 *     <li>a maximum number (>=1) of items is reached - {@link LimitedLinkedHashMap}</li>
 *     <li>the JVM needs memory - {@link SoftReference}</li>
 * </ul>
 */
public class ExpiringLimitedCache<K, V> implements AutoCloseable
{
	private static final Logger LOG = LoggerFactory.getLogger(ExpiringLimitedCache.class);
	
	protected final Duration expirationTime;
	protected final ScheduledExecutorService cleanUpExecutorService;
	
	protected ScheduledFuture<?> scheduledCleanUpFuture;
	protected final ReentrantLock cleanUpLock = new ReentrantLock();
	
	protected final Map<K, SoftReference<CacheValue<V>>> cache;
	
	public ExpiringLimitedCache(
		final Duration expirationTime,
		final int maxSize)
	{
		this(expirationTime, maxSize, DefaultHolder.instance());
	}
	
	public ExpiringLimitedCache(
		final Duration expirationTime,
		final int maxSize,
		final ScheduledExecutorService cleanUpExecutorService)
	{
		this.expirationTime = Objects.requireNonNull(expirationTime);
		if(expirationTime.toSeconds() < 1)
		{
			throw new IllegalStateException();
		}
		
		this.cleanUpExecutorService = cleanUpExecutorService;
		this.cache = Collections.synchronizedMap(new LimitedLinkedHashMap<>(maxSize));
	}
	
	public void put(final K key, final V value)
	{
		if(LOG.isTraceEnabled())
		{
			LOG.trace("put called for key[hashcode={}]: {}", key.hashCode(), key);
		}
		
		this.cache.put(key, new SoftReference<>(new CacheValue<>(value, currentUtcTime().plus(this.expirationTime))));
		this.startCleanupExecutorIfRequired();
	}
	
	public V get(final K key)
	{
		final String keyLogData = LOG.isTraceEnabled() ? "key[hashcode=" + key.hashCode() + "]" : null;
		LOG.trace("get called for {} : {}", keyLogData, key);
		
		final SoftReference<CacheValue<V>> ref = this.cache.get(key);
		if(ref == null)
		{
			LOG.trace("{} not in cache", keyLogData);
			return null;
		}
		final CacheValue<V> value = ref.get();
		if(value == null)
		{
			LOG.trace("Value for {} was disposed by GC", keyLogData);
			return null;
		}
		// Check if expired and remove
		if(value.isExpired())
		{
			LOG.trace("{} is expired", keyLogData);
			
			this.cache.remove(key);
			this.shutdownCleanupExecutorIfRequired();
			return null;
		}
		
		LOG.trace("{} is present", keyLogData);
		return value.value();
	}
	
	@SuppressWarnings("java:S2583") // Lock acquisition may take time
	private void startCleanupExecutorIfRequired()
	{
		if(this.scheduledCleanUpFuture != null)
		{
			return;
		}
		
		this.cleanUpLock.lock();
		try
		{
			// Recheck again
			if(this.scheduledCleanUpFuture != null)
			{
				return;
			}
			
			LOG.trace("Scheduling cleanup task");
			this.scheduledCleanUpFuture = this.cleanUpExecutorService.scheduleWithFixedDelay(
				this::runCleanup,
				this.expirationTime.toMillis(),
				this.expirationTime.toMillis() / 2,
				TimeUnit.MILLISECONDS);
		}
		finally
		{
			this.cleanUpLock.unlock();
		}
	}
	
	protected void runCleanup()
	{
		final long startTime = System.currentTimeMillis();
		
		// Collect to temporary list so that cache is not modified while reading
		final List<K> toClear = this.cache.entrySet()
			.stream()
			.filter(e -> Optional.ofNullable(e.getValue().get())
				.map(CacheValue::isExpired)
				.orElse(true))
			.map(Map.Entry::getKey)
			.toList();
		
		toClear.forEach(this.cache::remove);
		
		if(LOG.isTraceEnabled())
		{
			LOG.trace(
				"Cleared {}x cached entries, took {}ms",
				toClear.size(),
				System.currentTimeMillis() - startTime);
		}
		
		this.shutdownCleanupExecutorIfRequired();
	}
	
	protected void shutdownCleanupExecutorIfRequired()
	{
		if(!this.cache.isEmpty())
		{
			return;
		}
		
		this.shutdownCleanupExecutor();
	}
	
	@SuppressWarnings("java:S2583") // Lock acquisition may take a moment
	protected void shutdownCleanupExecutor()
	{
		if(this.scheduledCleanUpFuture == null)
		{
			return;
		}
		
		this.cleanUpLock.lock();
		try
		{
			// Recheck if this was changed in the meantime
			if(this.scheduledCleanUpFuture == null)
			{
				return;
			}
			LOG.trace("Stopping cleanup");
			this.scheduledCleanUpFuture.cancel(false);
			this.scheduledCleanUpFuture = null;
		}
		finally
		{
			this.cleanUpLock.unlock();
		}
	}
	
	public int cacheSize()
	{
		return this.cache.size();
	}
	
	protected static Instant currentUtcTime()
	{
		return Instant.now();
	}
	
	@Override
	public void close()
	{
		this.cache.clear();
		this.shutdownCleanupExecutor();
	}
	
	public record CacheValue<V>(
		V value,
		Instant utcCacheExpirationTime)
	{
		public CacheValue
		{
			Objects.requireNonNull(utcCacheExpirationTime);
		}
		
		public boolean isExpired()
		{
			return currentUtcTime().isAfter(this.utcCacheExpirationTime);
		}
	}
	
	
	@SuppressWarnings("java:S2160") // No fields or anything else was changed
	public static class LimitedLinkedHashMap<K, V> extends LinkedHashMap<K, V>
	{
		protected final int maxSize;
		
		public LimitedLinkedHashMap(final int maxSize)
		{
			if(maxSize < 1)
			{
				throw new IllegalStateException();
			}
			this.maxSize = maxSize;
		}
		
		@Override
		protected boolean removeEldestEntry(final Map.Entry<K, V> eldest)
		{
			return this.size() > this.maxSize;
		}
	}
}
