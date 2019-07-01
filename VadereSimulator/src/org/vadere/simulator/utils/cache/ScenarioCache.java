package org.vadere.simulator.utils.cache;

import org.vadere.simulator.projects.Scenario;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.CacheType;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellGridReadWriter;
import org.vadere.util.logging.Logger;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;

import tech.tablesaw.api.Table;

public class ScenarioCache {

	Logger logger = Logger.getLogger(ScenarioCache.class);

	private static final String CACHE_DIR_NAME = "__cache__";
	private static final String TARGET_FF = "_targetFF_";
	private static final String Distance_FF = "_distanceFF_";

	private boolean empty;
	final private Scenario scenario;
	private Path cachePath;
	private AttributesFloorField attFF;


	private HashMap<String, Table> floorFieldCache = new HashMap<>();
	private HashMap<String, CacheLoader> xxx= new HashMap<>();
	private String hash;





	public static ScenarioCache empty(){
		return new ScenarioCache();
	}

	public static ScenarioCache load(final Scenario scenario, Path cacheParentDir){
		return new ScenarioCache(scenario, cacheParentDir);
	}

	private ScenarioCache(){
		this.empty = true;
		this.scenario = null;
	}

	private ScenarioCache(Scenario scenario, Path scenarioPath){
		this.empty = scenario == null;
		this.scenario = scenario;

		if (!empty){
			Topography topography = scenario.getTopography();
			AttributesFloorField attFF = scenario.getModelAttributes()
					.stream()
					.filter(a -> a instanceof AttributesFloorField)
					.map(a ->(AttributesFloorField)a)
					.findFirst().orElse(null);
			if(attFF != null){
				this.attFF = attFF;
				this.hash = StateJsonConverter.getFloorFieldHash(topography, attFF);
				this.cachePath = scenarioPath.resolve(CACHE_DIR_NAME).resolve(attFF.getCacheDir());
				empty = !attFF.isUseCachedFloorField(); // deactivate cache object if caching is not active.
				if (!empty)
					findCacheOnFileSystem();

			} else {
				empty = true;
			}
		}
	}


	private void findCacheOnFileSystem(){
		// add target cache
		scenario.getTopography().getTargets().forEach(target -> {
			String cacheIdentifier = targetToIdentifier(target.getId());
			File file;
			if(attFF.getCacheType() == CacheType.CSV_CACHE){
				file = buildCsvCachePath(cacheIdentifier).toFile();
				if (file.exists()){
					addCsvCache(cacheIdentifier, file);
					logger.infof("found csv cache for target %d:  %s", target.getId(), file.toString());
				}
			} else {
				file = buildBinCachePath(cacheIdentifier).toFile();
				if (file.exists()) {
					addBinaryCache(cacheIdentifier, file);
					logger.infof("found binary cache for target %d:  %s", target.getId(), file.toString());
				}
			}
		});

		// add BruteForce DistFunction //todo should be configured in scenario
		String cacheIdentifier = distToIdentifier("BruteForce");
		File file;
		if(attFF.getCacheType() == CacheType.CSV_CACHE){
			file = buildCsvCachePath(cacheIdentifier).toFile();
			if (file.exists()){
				addCsvCache(cacheIdentifier, file);
				logger.infof("found csv cache for distance function %s", file.toString());
			}
		} else {
			file = buildBinCachePath(cacheIdentifier).toFile();
			if (file.exists()){
				addBinaryCache(cacheIdentifier, file);
				logger.infof("found binary cache for distance function %s", file.toString());
			}
		}
	}

	private Path buildCsvCachePath(String floorFieldIdentifier){
		return cachePath.resolve(hash +  floorFieldIdentifier + "_ffcache.csv");
	}

	private Path buildBinCachePath(String floorFieldIdentifier){
		return cachePath.resolve(hash +  floorFieldIdentifier + ".ffcache");
	}


	public String targetToIdentifier(int targetId){
		return TARGET_FF + targetId;
	}

	public String distToIdentifier(String name){
		return Distance_FF + name;
	}


	public ScenarioCache addCsvCache(String cacheIdentifier, InputStream stream){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		xxx.put(cacheIdentifier, cellGrid -> {
			try {
				CellGridReadWriter.read(cellGrid).fromCsv(stream);
			} catch (Exception e) {
				throw new CacheException("Cannot load cache from CSV InputStream", e);
			}
		});
		return 	this;
	}

	public ScenarioCache addCsvCache(String cacheIdentifier, File file){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		xxx.put(cacheIdentifier, cellGrid -> {
			try {
				CellGridReadWriter.read(cellGrid).fromCsv(file);
			} catch (Exception e) {
				throw new CacheException("Cannot load cache from CSV File: " + file.getAbsolutePath(), e);
			}
		});
		return 	this;
	}

	public ScenarioCache addBinaryCache(String cacheIdentifier, DataInputStream stream){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		xxx.put(cacheIdentifier, cellGrid -> {
			try {
				CellGridReadWriter.read(cellGrid).fromBinary(stream);
			} catch (Exception e) {
				throw new CacheException("Cannot load cache from Binary DataInputStream", e);
			}
		});
		return 	this;
	}

	public ScenarioCache addBinaryCache(String cacheIdentifier, File file){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		xxx.put(cacheIdentifier, cellGrid -> {
			try {
				CellGridReadWriter.read(cellGrid).fromBinary(file);
			} catch (Exception e) {
				throw new CacheException("Cannot load cache from Binary file: " + file.getAbsolutePath(), e);
			}
		});
		return 	this;
	}

	public void saveToCache(String cacheIdentifier, final CellGrid cellGrid){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		switch (attFF.getCacheType()){
			case BIN_CACHE:
				saveToBinCache(cacheIdentifier, cellGrid);
				break;
			case CSV_CACHE:
				saveToCsvCache(cacheIdentifier, cellGrid);
				break;
			default:
				throw new IllegalStateException("Must be either CSV or BIN cache");
		}
	}

	public void saveToCsvCache(String cacheIdentifier, final CellGrid cellGrid){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		File file = buildCsvCachePath(cacheIdentifier).toFile();
		try {
			CellGridReadWriter.write(cellGrid).toCsv(file);
		} catch (Exception e) {
			logger.errorf("cannot save cache %s", file.getAbsolutePath());
		}
	}

	public void saveToBinCache(String cacheIdentifier, final CellGrid cellGrid){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		File file = buildBinCachePath(cacheIdentifier).toFile();
		try {
			CellGridReadWriter.write(cellGrid).toBinary(file);
		} catch (Exception e) {
			logger.errorf("cannot save cache %s", file.getAbsolutePath());
		}
	}

	/**
	 * Retrun cache for given cacheIdentifier of null if no cache exists.
	 * @param cacheIdentifier
	 * @return
	 */
	public CacheLoader getCache(String cacheIdentifier){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		return xxx.getOrDefault(cacheIdentifier, null);
	}

	public CacheLoader getCacheForTarget(int targetId){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		return getCache(targetToIdentifier(targetId));
	}

	public CacheLoader getCacheForDistFunction(String distFunction){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		return getCache(distToIdentifier(distFunction));
	}



	public boolean isEmpty() {
		return empty;
	}

	public boolean isNotEmpty(){
		return  !empty;
	}
}
