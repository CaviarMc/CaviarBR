package fr.caviar.br.cache;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;

import fr.caviar.br.task.NativeTask;
import fr.caviar.br.task.UniversalTask;

abstract class ACache<T, U> {

	protected static final UniversalTask taskHandler = NativeTask.getInstance();

	@Nonnull
	protected Cache<T, U> objectsCached;
	@Nonnull
	private Function<T, Entry<U, ? extends Exception>> asyncGetObjectFunction;
	@Nonnull
	int timeBeforeRemove;
	@Nonnull
	TimeUnit unit;

	protected ACache(UnaryOperator<CacheBuilder<Object, Object>> builder, Function<T, Entry<U, ? extends Exception>> asyncGetObjectFunction, int timeBeforeRemove, TimeUnit unit) {
		this.objectsCached = builder.apply(CacheBuilder.newBuilder()).build();
		this.asyncGetObjectFunction = asyncGetObjectFunction;
		this.timeBeforeRemove = timeBeforeRemove;
		
		this.unit = unit;
	}

	protected ACache(Function<T, Entry<U, ? extends Exception>> asyncGetObjectFunction, int timeBeforeRemove, TimeUnit unit) {
		this(cb -> cb.recordStats().expireAfterAccess(timeBeforeRemove, unit), asyncGetObjectFunction, timeBeforeRemove, unit);
	}

	protected void privatePut(T key, U object) {
		objectsCached.put(key, object);
	}

	@Nullable
	public U getObjectCached(T key) {
		return objectsCached.getIfPresent(key);
	}
	
	protected void removeFromCache(T key) {
		objectsCached.invalidate(key);;
	}

	public ConcurrentMap<T, U> getObjectsCached() {
		return objectsCached.asMap();
	}

	public CacheStats getStats() {
		return objectsCached.stats();
	}

	/**
	 * @param key
	 * @param callback async if object is not in cache. Object U in callback can be null
	 * @return true if key is in cache
	 */
	public boolean get(T key, @Nullable BiConsumer<U, Exception> callback) {
		U u = getObjectCached(key);
		if (u != null) {
			if (callback != null)
				callback.accept(u, null);
			return true;
		} else {
			Runnable task = () -> {
				try {
					U u2 = getObjectNotCached(key);
					callback.accept(u2, null);
				} catch (Exception e) {
					callback.accept(null, e);
				}
			};
			if (Bukkit.isPrimaryThread())
				taskHandler.runTaskAsynchronously(task);
			else {
				task.run();
			}
			return false;
		}
	}

	/**
	 * Need to be async
	 * @throws BiConsumerException 
	 */
	@Nullable
	public U getObjectNotCached(T key) throws Exception  {
		U u;
		try {
			u = getObjectWithoutCached(key);
			if (u != null)
				privatePut(key, u);
		} catch (Exception e) {
			throw e;
		}
		return u;
	}

	/**
	 * Need to be async
	 * @return 
	 * @throws BiConsumerException 
	 */
	@Nullable
	protected U getObjectWithoutCached(T key) throws Exception {
		Entry<U, ? extends Exception> entry = asyncGetObjectFunction.apply(key);
		U u = entry.getKey();
		Exception exception = entry.getValue();
		if (exception != null)
			throw exception;
		return u;
	}

}
