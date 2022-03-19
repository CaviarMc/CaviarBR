package fr.caviar.br.cache;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
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

	protected static final UniversalTask task = NativeTask.getInstance();

	@Nonnull
	protected Cache<T, U> objectsCached;
	@Nonnull
	private BiConsumer<T, BiConsumer<U, Exception>> asyncGetObjectFunction;
	@Nonnull
	int timeBeforeRemove;
	@Nonnull
	TimeUnit unit;

	protected ACache(UnaryOperator<CacheBuilder<Object, Object>> builder, BiConsumer<T, BiConsumer<U, Exception>> asyncGetObjectFunction, int timeBeforeRemove, TimeUnit unit) {
		this.objectsCached = builder.apply(CacheBuilder.newBuilder()).build();
		this.asyncGetObjectFunction = asyncGetObjectFunction;
		this.timeBeforeRemove = timeBeforeRemove;
		this.unit = unit;
	}

	protected ACache(BiConsumer<T, BiConsumer<U, Exception>> asyncGetObjectFunction, int timeBeforeRemove, TimeUnit unit) {
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
			if (Bukkit.isPrimaryThread())
				task.runTaskAsynchronously(() -> {
					getObjectNotCached(key, callback);
				});
			else
				getObjectNotCached(key, callback);
			return false;
		}
	}

	/**
	 * Need to be async
	 * @throws BiConsumerException 
	 */
	@Nullable
	public void getObjectNotCached(T key, @Nullable BiConsumer<U, Exception> result) {
		asyncGetObjectFunction.accept(key, (u, exception) -> {
			if (u != null)
				privatePut(key, u);
			if (result != null)
				result.accept(u, exception);
		});
	}

	/**
	 * Need to be async
	 * @throws BiConsumerException 
	 */
	@Nullable
	protected void getObjectWithoutCached(T key, @Nonnull BiConsumer<U, Exception> result) {
		U u = getObjectCached(key);
		if (u != null)
			result.accept(u, null);
		else
			getObjectNotCached(key, result);
	}

}
