package org.vadere.simulator.utils.cache;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

import org.vadere.simulator.projects.Scenario;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.CacheType;
import org.vadere.state.types.EikonalSolverType;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class ScenarioCache {

	private static Logger logger = Logger.getLogger(ScenarioCache.class);

	private static final String CACHE_DIR_NAME = "__cache__";
	private static final String TARGET_FF = "_targetFF_";
	private static final String Distance_FF = "_distanceFF_";
	private static final String txt_sufix = ".txt";
	private static final String bin_sufix = ".ffcache";

	private boolean empty;
	final private Scenario scenario;
	private Path cachePath;
	private AttributesFloorField attFF;

	private HashMap<String, ICacheObject> cacheMap = new HashMap<>();
	private String hash;

	public static ScenarioCache empty(){
		return new ScenarioCache();
	}

	public static ScenarioCache load(final Scenario scenario, Path cacheParentDir){
		return new ScenarioCache(scenario, cacheParentDir);
	}

	public static String getHash(final Scenario scenario){
		Topography topography = scenario.getTopography();
		AttributesFloorField attFF = scenario.getModelAttributes()
				.stream()
				.filter(a -> a instanceof AttributesFloorField)
				.map(a ->(AttributesFloorField)a)
				.findFirst().orElse(null);
		if(attFF != null) {
			return StateJsonConverter.getFloorFieldHash(topography, attFF);
		}
		return null;
	}

	private ScenarioCache(){
		this.empty = true;
		this.scenario = null;
	}

	private Path resolveCachePath(Path cacheParentDir, String scenarioFileConfig){
		Path scenarioConfig = Paths.get(scenarioFileConfig);

		// check if scenario file specifies an absolute path. --> If yes use it.
		if (scenarioConfig.isAbsolute()){
			logger.infof("Cache path overwritten: Scenario specified absolute cache path. %s",
					scenarioConfig.toAbsolutePath().toString());
			return scenarioConfig.toAbsolutePath();
		}

		Path base;
		// scenario file uses relative path. Check config for global override
		if (VadereConfig.getConfig().getBoolean("Vadere.cache.useGlobalCacheBaseDir", false)){
			logger.info("Global override for cache location found.");
			base = Paths.get(VadereConfig.getConfig().getString("Vadere.cache.globalCacheBaseDir"));
		} else {
			base = cacheParentDir.resolve(CACHE_DIR_NAME);
		}

		Path ret = base.resolve(scenarioConfig);
		logger.infof("Cache search location: %s", ret.toAbsolutePath());
		return ret.toAbsolutePath();
	}

	private ScenarioCache(Scenario scenario, Path cacheParentDir){
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
				this.cachePath = resolveCachePath(cacheParentDir, attFF.getCacheDir()); //cacheParentDir.resolve(CACHE_DIR_NAME).resolve(attFF.getCacheDir());
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
			addCacheByType(attFF.getCreateMethod(), attFF.getCacheType(), cacheIdentifier);
		});

		// add BruteForce DistFunction //todo should be configured in scenario
		String cacheIdentifier = distToIdentifier("BruteForce");
		addCacheByType(attFF.getCreateMethod(), attFF.getCacheType(), cacheIdentifier);
	}

	private void addCacheByType(EikonalSolverType eikType, CacheType cacheType, String cacheIdentifier){

		File file;
		if (eikType.isUsingCellGrid()){
			if(cacheType == CacheType.TXT_CACHE){
				file = buildCsvCachePath(cacheIdentifier).toFile();
				cacheMap.put(cacheIdentifier, new CellGridTxtCacheObject(cacheIdentifier, file));
			} else {
				file = buildBinCachePath(cacheIdentifier).toFile();
				cacheMap.put(cacheIdentifier, new CellGridBinaryCacheObject(cacheIdentifier, file));
			}
		} else {
			if(cacheType == CacheType.TXT_CACHE){
				file = buildCsvCachePath(cacheIdentifier).toFile();
				cacheMap.put(cacheIdentifier, new MeshTxtCacheObject(cacheIdentifier, file));
			}
		}
	}

	private Path buildCsvCachePath(String floorFieldIdentifier){
		return cachePath.resolve(hash +  floorFieldIdentifier + txt_sufix);
	}

	private Path buildBinCachePath(String floorFieldIdentifier){
		return cachePath.resolve(hash +  floorFieldIdentifier + bin_sufix);
	}

	public String targetToIdentifier(int targetId){
		return TARGET_FF + targetId;
	}

	public String distToIdentifier(String name){
		return Distance_FF + name;
	}


	public ScenarioCache addReadOnlyCache(String cacheIdentifier, ByteArrayInputStream stream){
		if(empty)
			throw new IllegalStateException("Empty cache object.");
		switch (attFF.getCacheType()) {
			case BIN_CACHE:
				cacheMap.put(cacheIdentifier, new CellGridBinaryCacheObject(cacheIdentifier, null, new DataInputStream(new FastBufferedInputStream(stream))));
				logger.infof("binary cache loaded for identifier: %s", cacheIdentifier);
				break;
			case TXT_CACHE:
				cacheMap.put(cacheIdentifier, new CellGridTxtCacheObject(cacheIdentifier, null, stream));
				logger.infof("csv cache loaded for identifier: %s", cacheIdentifier);
				break;
			default:
				throw new IllegalStateException("Must be either CSV or BIN cache");
		}
		return this;
	}

	/**
	 * Retrun cache for given cacheIdentifier of null if no cache readable.
	 * @param cacheIdentifier
	 * @return
	 */
	public ICacheObject getCache(String cacheIdentifier){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		return cacheMap.getOrDefault(cacheIdentifier, ICacheObject.empty(cacheIdentifier));
	}

	public ICacheObject getCacheForTarget(int targetId){
		if (empty)
			throw new IllegalStateException("Empty cache object.");
		return getCache(targetToIdentifier(targetId));
	}

	public ICacheObject getCacheForDistFunction(String distFunction){
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
