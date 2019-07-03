package org.vadere.simulator.utils.cache;

public interface CacheLoader<T> {

	void loadCacheFor(T data) throws CacheException;

}
