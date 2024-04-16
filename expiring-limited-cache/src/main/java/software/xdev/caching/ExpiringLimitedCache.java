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
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


/**
 * Caches values until
 * <ul>
 *     <li>a specific time (>=1s) elapses (is cleared in batches by a timer so it may stick around a bit longer)
 *     - {@link #expirationTime}</li>
 *     <li>a maximum number (>=1) of items is reached - {@link LimitedLinkedHashMap}</li>
 *     <li>the JVM needs memory - {@link SoftReference})</li>
 * </ul>
 */
public class ExpiringLimitedCache<K, V>
{
	protected final Duration expirationTime;
	
	protected final AtomicInteger cleanUpExecutorCounter = new AtomicInteger(1);
	protected final ThreadFactory cleanUpExecutorThreadFactory;
	protected ScheduledExecutorService cleanUpExecutor;
	protected final Object cleanUpExecutorLock = new Object();
	
	protected Consumer<String> logConsumer;
	
	protected final Map<K, SoftReference<CacheValue<V>>> cache;
	
	public ExpiringLimitedCache(
		final String cacheName,
		final Duration expirationTime,
		final int maxSize)
	{
		this.expirationTime = Objects.requireNonNull(expirationTime);
		if(expirationTime.toSeconds() < 1)
		{
			throw new IllegalStateException();
		}
		
		this.cache = Collections.synchronizedMap(new LimitedLinkedHashMap<>(maxSize));
		
		this.cleanUpExecutorThreadFactory = r -> {
			final Thread thread = new Thread(r);
			thread.setName(cacheName + "-Cache-Cleanup-Executor-" + this.cleanUpExecutorCounter.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		};
	}
	
	public void setLogConsumer(final Consumer<String> logConsumer)
	{
		this.logConsumer = logConsumer;
	}
	
	protected void log(final String s)
	{
		if(this.logConsumer != null)
		{
			this.logConsumer.accept(s);
		}
	}
	
	public void put(final K key, final V value)
	{
		this.log("put called for key[hashcode=" + key.hashCode() + "]: " + key);
		
		this.cache.put(key, new SoftReference<>(new CacheValue<>(value, currentUtcTime().plus(this.expirationTime))));
		this.startCleanupExecutorIfRequired();
	}
	
	public V get(final K key)
	{
		final String keyLogStr = "key[hashcode=" + key.hashCode() + "]";
		this.log("get called for " + keyLogStr + ": " + key);
		
		final SoftReference<CacheValue<V>> ref = this.cache.get(key);
		if(ref == null)
		{
			this.log(keyLogStr + " not in cache");
			return null;
		}
		final CacheValue<V> value = ref.get();
		if(value == null)
		{
			this.log("Value for " + keyLogStr + " was disposed by GC");
			return null;
		}
		// Check if expired and remove
		if(value.isExpired())
		{
			this.log(keyLogStr + " is expired");
			
			this.cache.remove(key);
			this.shutdownCleanupExecutorIfRequired();
			return null;
		}
		
		this.log(keyLogStr + " is present");
		return value.value();
	}
	
	private synchronized void startCleanupExecutorIfRequired()
	{
		if(this.cleanUpExecutor != null)
		{
			return;
		}
		synchronized(this.cleanUpExecutorLock)
		{
			if(this.cleanUpExecutor != null)
			{
				return;
			}
			
			this.log("Starting cleanupExecutor");
			this.cleanUpExecutor = Executors.newScheduledThreadPool(1, this.cleanUpExecutorThreadFactory);
			this.cleanUpExecutor.scheduleAtFixedRate(
				this::runCleanup,
				this.expirationTime.toMillis(),
				this.expirationTime.toMillis() / 2,
				TimeUnit.MILLISECONDS);
		}
	}
	
	private void runCleanup()
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
		
		this.log("Cleared " + toClear.size() + "x cached entries, took "
			+ (System.currentTimeMillis() - startTime) + "ms");
		
		this.shutdownCleanupExecutorIfRequired();
	}
	
	protected void shutdownCleanupExecutorIfRequired()
	{
		if(this.cache.isEmpty())
		{
			synchronized(this.cleanUpExecutorLock)
			{
				this.log("Shutting down cleanupExecutor");
				this.cleanUpExecutor.shutdownNow();
				this.cleanUpExecutor = null;
			}
		}
	}
	
	public int cacheSize()
	{
		return this.cache.size();
	}
	
	protected static LocalDateTime currentUtcTime()
	{
		return LocalDateTime.now(Clock.systemUTC());
	}
	
	public record CacheValue<V>(
		V value,
		LocalDateTime utcCacheExpirationTime)
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
