package fr.caviar.br.cache;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import fr.caviar.br.utils.Utils.BiConsumerCanFail;

public class BasicCache<K, T> extends ACache<K, T> {

	/**
	 * Create a map with cached objects and key and and a way to recover them through @asyncGetObjectFunction if they not in cache.
	 */
	public BasicCache(BiConsumerCanFail<K, Consumer<T>> asyncGetObjectFunction, int timeBeforeRemove, TimeUnit unit) {
		super(asyncGetObjectFunction, timeBeforeRemove, unit);
	}

	public void put(K key, T object) {
		objectsCached.put(key, object);
	}

}
