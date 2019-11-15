package org.vadere.simulator.utils.cache;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellGridReadWriter;
import org.vadere.util.logging.Logger;

import java.io.File;
import java.io.InputStream;

public class CellGridTxtCacheObject extends AbstractCacheObject implements ICellGridCacheObject{
	private  static Logger logger = Logger.getLogger(CellGridTxtCacheObject.class);

	public CellGridTxtCacheObject(String cacheIdentifier, File cacheLocation){
		super(cacheIdentifier, cacheLocation);
	}

	public CellGridTxtCacheObject(String cacheIdentifier, File cacheLocation, InputStream inputStream) {
		super(cacheIdentifier, cacheLocation, inputStream);
	}

	@Override
	public void initializeObjectFromCache(CellGrid object) throws CacheException {
		try {
			CellGridReadWriter.read(object).fromTextFile(new FastBufferedInputStream(inputStream));
		} catch (Exception e) {
			throw new CacheException("Cannot load cache from TXT InputStream", e);
		}
	}

	@Override
	public void persistObject(CellGrid object) throws CacheException {
		try {
			logger.infof("write cache: %s", getCacheLocation());
			CellGridReadWriter.write(object).toTextFile(cacheLocation);
		} catch (Exception e) {
			logger.errorf("cannot save cache %s", cacheLocation.getAbsolutePath());
		}
	}

	@Override
	public String getCacheLocation() {
		return cacheLocation.getAbsolutePath().toString();
	}

}
