package org.vadere.simulator.utils.cache;

import org.junit.Test;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.reflection.TestResourceHandlerScenario;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Target;
import org.vadere.state.types.CacheType;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ScenarioCacheTest implements TestResourceHandlerScenario {

	@Override
	public Path getTestDir() {
		return getPathFromResources("/data/cache");
	}

	@Test
	public void testNOcache(){
		Scenario s = getScenarioFromRelativeResource("s003.scenario");
		assertThat(getAttrFF(s).getCacheType(), equalTo(CacheType.NO_CACHE));

		ScenarioCache scenarioCache = ScenarioCache.load(s, getTestDir().toAbsolutePath());
		assertThat(scenarioCache.isEmpty(), equalTo(true));
	}

	@Test
	public void testTXTcache(){
		Scenario s = getScenarioFromRelativeResource("s001.scenario");
		assertThat(getAttrFF(s).getCacheType(), equalTo(CacheType.TXT_CACHE));

		ScenarioCache scenarioCache = ScenarioCache.load(s, getTestDir().toAbsolutePath());
		assertThat(scenarioCache.isEmpty(), equalTo(false));

		for (Target target : s.getTopography().getTargets()) {
			ICacheObject cacheObject = scenarioCache.getCacheForTarget(target.getId());

			assertThat(cacheObject instanceof ICellGridCacheObject, equalTo(true));
			assertThat(cacheObject instanceof CellGridTxtCacheObject, equalTo(true));
			assertThat(cacheObject.writable(), equalTo(true));
		}

		// ensure none existing identifier returns empty cache object
		ICacheObject cacheObject = scenarioCache.getCache("doesNotExist");
		assertThat(cacheObject.readable(), equalTo(false));
		assertThat(cacheObject.writable(), equalTo(false));
		assertThat(cacheObject.getCacheIdentifier(), equalTo("doesNotExist"));

	}

	@Test
	public void testBinCache(){
		Scenario s = getScenarioFromRelativeResource("s002.scenario");
		AttributesFloorField attr = getAttrFF(s);
		assertThat(attr.getCacheType(), equalTo(CacheType.BIN_CACHE));

		ScenarioCache scenarioCache = ScenarioCache.load(s, getTestDir().toAbsolutePath());
		assertThat("There should be a cache object present",scenarioCache.isEmpty(), equalTo(false));

		for (Target target : s.getTopography().getTargets()) {
			ICacheObject cacheObject = scenarioCache.getCacheForTarget(target.getId());

			assertThat(cacheObject instanceof ICellGridCacheObject, equalTo(true));
			assertThat(cacheObject instanceof CellGridBinaryCacheObject, equalTo(true));
			assertThat(cacheObject.writable(), equalTo(true));
		}
	}

	@Test
	public void testManualBinCache(){
		HashMap<String, ByteArrayInputStream> cacheInput= new HashMap<>();
		cacheInput.put("target1", new ByteArrayInputStream(new byte[10]));
		cacheInput.put("target2", new ByteArrayInputStream(new byte[10]));

		Scenario s = getScenarioFromRelativeResource("s002.scenario"); // binary cache
		ScenarioCache scenarioCache = ScenarioCache.load(s, getTestDir().toAbsolutePath());
		cacheInput.forEach(scenarioCache::addReadOnlyCache);

		cacheInput.entrySet().forEach(e -> {
			ICacheObject cacheObject = scenarioCache.getCache(e.getKey());
			assertThat(cacheObject.writable(), equalTo(false));
			assertThat(cacheObject.readable(), equalTo(true));
			assertThat(cacheObject.getCacheIdentifier(), equalTo(e.getKey()));
			assertThat(cacheObject instanceof CellGridBinaryCacheObject, equalTo(true));
		});
	}

	private AttributesFloorField getAttrFF(Scenario s){
		AttributesFloorField attr = (AttributesFloorField) s.getModelAttributes().stream().filter(a-> a instanceof AttributesFloorField).findAny().orElse(null);
		if (attr == null)
			fail("scenario should have AttributesFloorfield");

		return  attr;
	}

}