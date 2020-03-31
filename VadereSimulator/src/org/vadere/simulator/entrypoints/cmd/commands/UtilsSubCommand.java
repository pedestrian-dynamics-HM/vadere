package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.util.version.Version;
import org.vadere.simulator.entrypoints.cmd.SubCommandRunner;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.fields.PotentialFieldDistancesBruteForce;
import org.vadere.simulator.models.potential.solver.EikonalSolverCacheProvider;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Target;
import org.vadere.state.types.CacheType;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;


public class UtilsSubCommand implements SubCommandRunner {

	private final static Logger logger = Logger.getLogger(UtilsSubCommand.class);

	private HashMap<String, Pair<String, SubCommandRunner>> methods;

	public UtilsSubCommand() {
		methods = new HashMap<>();
		methods.put("getHash", Pair.of("[-i: file, -o: ignored]", this::getHash));
		methods.put("binCache", Pair.of("[-i: file, -o: directory]",this::calculateBinCache));
		methods.put("txtCache", Pair.of("[-i: file, -o: directory]",this::calculateTextCache));
	}

	public String[] methodsString(){
		Set<String> mSet = methods.keySet();
		String[] ret = new String[mSet.size()];
		mSet.toArray(ret);
		return ret;
	}

	public String methodHelp(){
		StringBuilder sb = new StringBuilder();
		methods.forEach((key, value) -> sb.append("\n --> ").append(key).append(":").append(value.getLeft()));
		return sb.toString();
	}

	private Scenario createScenario(String path) throws IOException {
		Scenario scenario = null;

		path = path.replace("~", System.getProperty("user.home"));
		try {
			scenario = ScenarioFactory.createScenarioWithScenarioFilePath(Paths.get(path));
		} catch (IOException e) {
			logger.errorf("cannot read scenario from %s", Paths.get(path).toAbsolutePath().toString());
			System.exit(-1);
		}
		return scenario;
	}


	@Override
	public void run(Namespace ns, ArgumentParser parser) throws Exception {

		String m = ns.get("method");
		SubCommandRunner runner = methods.get(m).getRight();
		if (runner != null){
			runner.run(ns, parser);
		} else {
			logger.errorf("no method found with name %s", m );
			System.exit(-1);
		}
	}


	/**
	 *  Return the hash used to check if a new cache value needed to be calculated.
	 */
	private void getHash(Namespace ns, ArgumentParser parser) throws Exception{
		calculateCache(ns, parser, CacheType.TXT_CACHE);
	}

	private void calculateBinCache(Namespace ns, ArgumentParser parser) throws Exception{
		calculateCache(ns, parser, CacheType.BIN_CACHE);
	}

	private void calculateTextCache(Namespace ns, ArgumentParser parser) throws Exception{
		calculateCache(ns, parser, CacheType.TXT_CACHE);
	}


	/**
	 * 	Recalculated cache and save to given location. This method does not lookup any preexisting
	 * 	cache files anywhere on the system. Only existing files in the output folder will be checked.
	 *
	 * 	To ensure recalculation use a clean output directory
	 */
	private void calculateCache(Namespace ns, ArgumentParser parser, CacheType cacheType) throws Exception{
		Scenario scenario = createScenario(ns.getString("input"));
		if (ns.getString("output") == null){
			logger.errorf("need output folder for this method");
			System.exit(-1);
		}
		Path out = Paths.get(ns.getString("output"));
		if (out.toFile().isFile()){
			logger.errorf("given output is an existing file. This method needs a directory.");
			System.exit(-1);
		}
		logger.infof("Scenario: %s with hash: %s", scenario.getName(), ScenarioCache.getHash(scenario));


		AttributesFloorField attFF = scenario.getModelAttributes()
				.stream()
				.filter(a -> a instanceof AttributesFloorField)
				.map(a ->(AttributesFloorField)a)
				.findFirst().orElse(null);
		if (attFF == null){
			logger.errorf("Given scenario has no floor field attributes");
			System.exit(-1);
		}

		// override cache location and type with given absolute values.
		attFF.setCacheDir(out.toAbsolutePath().toString());
		attFF.setCacheType(cacheType);

		logger.infof("write Distance cache");
		ScenarioCache cache = ScenarioCache.load(scenario, out.toAbsolutePath());
		IPotentialField distanceField = new PotentialFieldDistancesBruteForce(
				scenario.getTopography().getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()),
				new VRectangle(scenario.getTopography().getBounds()),
				new AttributesFloorField(), cache);

		EikonalSolverCacheProvider provider = new EikonalSolverCacheProvider(cache);
		for (Target target : scenario.getTopography().getTargets()) {
			logger.infof("write cache for target %s", target.getId());
			//TODO: load the mesh if there is one
			provider.provide(new Domain(scenario.getTopography())
					, target.getId()
					, scenario.getTopography().getTargetShapes().get(target.getId())
					, scenario.getTopography().getAttributesPedestrian()
					, attFF);
		}

	}
}
