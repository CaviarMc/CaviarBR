package fr.caviar.br.cache;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;

import fr.caviar.br.task.NativeTask;
import fr.caviar.br.task.UniversalTask;
import fr.caviar.br.utils.Utils.BiConsumerCanFail;
import fr.caviar.br.utils.Utils.BiConsumerCanFail.BiConsumerException;

abstract class ACache<T, U> {

	protected static final UniversalTask task = NativeTask.getInstance();

	@Nonnull
	protected Cache<T, U> objectsCached;
	@Nonnull
	private BiConsumerCanFail<T, Consumer<U>> asyncGetObjectFunction;
	@Nonnull
	int timeBeforeRemove;
	@Nonnull
	TimeUnit unit;

	protected ACache(UnaryOperator<CacheBuilder<Object, Object>> builder, BiConsumerCanFail<T, Consumer<U>> asyncGetObjectFunction, int timeBeforeRemove, TimeUnit unit) {
		this.objectsCached = builder.apply(CacheBuilder.newBuilder()).build();
		this.asyncGetObjectFunction = asyncGetObjectFunction;
		this.timeBeforeRemove = timeBeforeRemove;
		this.unit = unit;
	}

	protected ACache(BiConsumerCanFail<T, Consumer<U>> asyncGetObjectFunction, int timeBeforeRemove, TimeUnit unit) {
		this(cb -> cb.recordStats().expireAfterAccess(timeBeforeRemove, unit), asyncGetObjectFunction, timeBeforeRemove, unit);
	}

	private void privatePut(T key, U object) {
		objectsCached.put(key, object);
	}

	@Nullable
	public U getObjectCached(T key) {
		return objectsCached.getIfPresent(key);
	}

	/**
	 * Need to be async
	 * @throws BiConsumerException 
	 */
	@Nullable
	protected void getObjectNotCached(T key, @Nullable Consumer<U> result) throws BiConsumerException {
		asyncGetObjectFunction.acceptException(key, t -> {
			if (t != null)
				privatePut(key, t);
			if (result != null)
				result.accept(t);
		});
	}

	/**
	 * Need to be async
	 * @throws BiConsumerException 
	 */
	@Nullable
	protected void getObjectWithoutCached(T key, @Nonnull Consumer<U> result) throws BiConsumerException {
		U u = getObjectCached(key);
		if (u != null)
			result.accept(u);
		else
			getObjectNotCached(key, result);
	}

	/**
	 *
	 * @param key
	 * @param callback async if object is not in cache. Object U in callback can be null
	 * @return true if key is in cache
	 */
	public boolean get(T key, @Nullable Consumer<U> callback) {
		U u = getObjectCached(key);
		if (u != null) {
			if (callback != null)
				callback.accept(u);
			return true;
		} else {
			task.runTaskAsynchronously(() -> {
				try {
					getObjectNotCached(key, callback);
				} catch (BiConsumerException e) {
					e.printStackTrace();
				}
			});
			return false;
		}
	}

	public ConcurrentMap<T, U> getObjectsCached() {
		return objectsCached.asMap();
	}

	public CacheStats getStats() {
		return objectsCached.stats();
	}
}
