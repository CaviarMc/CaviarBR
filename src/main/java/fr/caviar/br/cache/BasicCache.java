package fr.caviar.br.cache;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class BasicCache<T, U> extends ACache<T, U> {

	/**
	 * Create a map with cached objects and key and and a way to recover them through @asyncGetObjectFunction if they not in cache.
	 */
	public BasicCache(BiConsumer<T, BiConsumer<U, Exception>> asyncGetObjectFunction, int timeBeforeRemove, TimeUnit unit) {
		super(asyncGetObjectFunction, timeBeforeRemove, unit);
	}

	public void put(T key, U object) {
		objectsCached.put(key, object);
	}

}
