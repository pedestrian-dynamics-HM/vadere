package org.vadere.simulator.utils.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public abstract class AbstractCacheObject {

	protected File cacheLocation;
	protected InputStream inputStream;
	protected String cacheIdentifier;

	public AbstractCacheObject(String cacheIdentifier, File cacheLocation){
		this.cacheIdentifier = cacheIdentifier;
		this.cacheLocation = cacheLocation;
		try {
			this.inputStream = new FileInputStream(cacheLocation);
		} catch (FileNotFoundException e) {
			this.inputStream = null;
		}

	}

	public AbstractCacheObject(String cacheIdentifier, File cacheLocation, InputStream inputStream) {
		this.cacheIdentifier = cacheIdentifier;
		this.cacheLocation = cacheLocation;
		this.inputStream = inputStream;
	}


	public boolean readable() {
		return inputStream!=null;
	}

	public boolean writable() {
		return cacheLocation != null && !cacheLocation.isDirectory();
	}

	public String getCacheIdentifier() {
		return cacheIdentifier;
	}
}
