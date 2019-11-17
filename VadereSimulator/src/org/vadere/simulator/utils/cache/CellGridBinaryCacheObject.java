package org.vadere.simulator.utils.cache;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellGridReadWriter;
import org.vadere.util.logging.Logger;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;

public class CellGridBinaryCacheObject extends AbstractCacheObject implements ICellGridCacheObject {

	private  static Logger logger = Logger.getLogger(CellGridBinaryCacheObject.class);

	public CellGridBinaryCacheObject(String cacheIdentifier, File cacheLocation){
		super(cacheIdentifier, cacheLocation);
	}

	public CellGridBinaryCacheObject(String cacheIdentifier, File cacheLocation, InputStream inputStream) {
		super(cacheIdentifier, cacheLocation, inputStream);
	}

	@Override
	public void initializeObjectFromCache(CellGrid object) throws CacheException {
		try {
			logger.infof("read cache: %s", getCacheLocation());
			CellGridReadWriter.read(object).fromBinary(new DataInputStream(new FastBufferedInputStream(inputStream)));
		} catch (Exception e) {
			throw new CacheException("Cannot load cache from BIN InputStream", e);
		}
	}

	@Override
	public void persistObject(CellGrid object) throws CacheException {
		try {
			CellGridReadWriter.write(object).toBinary(cacheLocation);
		} catch (Exception e) {
			logger.errorf("cannot save cache %s", cacheLocation.getAbsolutePath());
		}
	}

	@Override
	public String getCacheLocation() {

		if (cacheLocation != null ){
			return cacheLocation.getAbsolutePath();
		} else {
			return "provided via TraCI";
		}
	}
}
