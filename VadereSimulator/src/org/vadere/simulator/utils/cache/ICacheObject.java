package org.vadere.simulator.utils.cache;

import org.vadere.util.logging.Logger;

public interface ICacheObject<T> {
	static Logger logger = Logger.getLogger(ICacheObject.class);

	static ICacheObject empty(String cacheIdentifier){
		return new ICacheObject() {
			@Override
			public void initializeObjectFromCache(Object object) throws CacheException {
				logger.errorf("initializeObjectFromCache called on empty cacheObject. cacheIdentifier: %s", getCacheIdentifier());
			}

			@Override
			public void persistObject(Object object) throws CacheException {
				logger.errorf("persistObject called on empty cacheObject cacheIdentifier: %s", getCacheIdentifier());
			}

			@Override
			public boolean readable() {
				return false;
			}

			@Override
			public boolean writable() {
				return false;
			}

			@Override
			public String getCacheLocation() {
				return "";
			}

			@Override
			public String getCacheIdentifier() {
				return cacheIdentifier;
			}
		};
	}

	void initializeObjectFromCache(T object) throws CacheException;
	void persistObject(T object) throws CacheException;
	boolean readable();
	boolean writable();
	String getCacheLocation();

	String getCacheIdentifier();

}
