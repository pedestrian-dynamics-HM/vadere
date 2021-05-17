package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.utils.MeshConstructor;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.meshing.utils.io.poly.PSLGGenerator;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.migration.GeometryCleaner;
import org.vadere.simulator.utils.pslg.PSLGConverter;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.scenario.Obstacle;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.io.IOUtils;
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

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class UtilsSubCommand implements SubCommandRunner{

	private final static Logger logger = Logger.getLogger(UtilsSubCommand.class);

	private HashMap<String,CommandWrapper> methods;

	private static HashMap<String, Object> args(Object... args) throws Exception {
		if(args.length % 2 != 0){
			throw new Exception("expected even number of object to build args HashMap");
		}
		HashMap<String, Object> _args = new HashMap<>();
		for(int k=0; k < args.length-1; k=k+2){
			int v = k + 1;
			if(!(args[k] instanceof String)){
				throw new Exception("Only String keys allowed");
			}
			_args.put((String)args[k], args[v]);
		}
		return _args;
	}

	public UtilsSubCommand() throws Exception {
		methods = new HashMap<>();
		methods.put("getHash", new CommandWrapper(this::getHash, "-i: file, -o: ignored", null));
		methods.put("binCache", new CommandWrapper(this::calculateBinCache, "-i: file, -o: directory", null));
		methods.put("txtCache", new CommandWrapper(this::calculateTextCache, "-i: file, -o: directory", null));
		methods.put("mergeObstacles", new CommandWrapper(this::mergeObstacles, "-i: file, -o: directory", args("tolerance", 0.1)));
		methods.put("createBackgroundMesh", new CommandWrapper(this::createBackgroundMesh, "-i: file", args("hmin", 2.0, "hmax", 5.0, "override", true)));
	}

	public String[] methodsString(){
		Set<String> mSet = methods.keySet();
		String[] ret = new String[mSet.size()];
		mSet.toArray(ret);
		return ret;
	}

	public String methodHelp(){
		StringBuilder sb = new StringBuilder();
		methods.forEach((key, value) -> sb.append("\n --> ").append(key).append(":").append(value.getHelp()));
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
	public void run(Namespace ns, ArgumentParser parser, HashMap<String, Object> args) throws Exception {

		String m = ns.get("method");
		CommandWrapper runner = methods.get(m);
		if (runner != null){
			// CommandWrapper has args
			runner.run(ns, parser);
		} else {
			logger.errorf("no method found with name %s", m );
			System.exit(-1);
		}
	}


	/**
	 *  Return the hash used to check if a new cache value needed to be calculated.
	 */
	private void getHash(Namespace ns, ArgumentParser parser, HashMap<String, Object> args) throws Exception{
		calculateCache(ns, parser, CacheType.TXT_CACHE);
	}

	private void calculateBinCache(Namespace ns, ArgumentParser parser, HashMap<String, Object> args) throws Exception{
		calculateCache(ns, parser, CacheType.BIN_CACHE);
	}

	private void calculateTextCache(Namespace ns, ArgumentParser parser, HashMap<String, Object> args) throws Exception{
		calculateCache(ns, parser, CacheType.TXT_CACHE);
	}

	private void mergeObstacles(Namespace ns, ArgumentParser parser, HashMap<String, Object> args) throws Exception {
		Scenario scenario = createScenario(ns.getString("input"));

		List<Obstacle> before = new ArrayList<>(scenario.getTopography().getObstacles());

		List<VPolygon> polygons = before.stream()
				.map(obstacle -> obstacle.getShape())
				.map(shape -> shape instanceof VRectangle ? new VPolygon(shape) : shape)
				.filter(shape -> shape instanceof VPolygon)
				.map(shape -> ((VPolygon)shape))
				.collect(Collectors.toList());

		GeometryCleaner topographyCleaner = new GeometryCleaner(
				new VRectangle(scenario.getTopography().getBounds()), polygons, 0.1);
		logger.debug("foo");
		logger.info("start merging, this can require some time.");
		Pair<VPolygon, List<VPolygon>> mergedPolygons = topographyCleaner.clean();
		//polygons = WeilerAtherton.magnet(polygons, new VRectangle(getScenarioPanelModel().getBounds()));
		logger.info("merging process finisehd.");

		// remove polygon obstacles
		List<Obstacle> polyObs = scenario.getTopography().getObstacles().stream()
				.filter(o -> o.getShape() instanceof VPolygon || o.getShape() instanceof  VRectangle).collect(Collectors.toList());
		for(Obstacle o : polyObs){
			scenario.getTopography().getObstacles().remove(o);
		}

		// add merged obstacles
		mergedPolygons.getRight()
				.stream()
				.map(polygon -> new Obstacle(new AttributesObstacle(-1, polygon)))
				.forEach(obstacle -> scenario.getTopography().addObstacle(obstacle));

		scenario.getTopography().generateUniqueIdIfNotSet();
		logger.info("write cleaned sceanrio.");
		IOUtils.writeTextFile(
				ns.getString("output"),
				JsonConverter.serializeScenarioRunManager(scenario));
	}

	private void createBackgroundMesh(Namespace ns, ArgumentParser parser, HashMap<String, Object> args) throws Exception {
		double hmin = (double)args.get("hmin");
		double hmax = (double)args.get("hmax");
		Scenario scenario = createScenario(ns.getString("input"));
		Path outDir = Paths.get((String)ns.get("input")).getParent();
		String scenarioName = scenario.getName();

		logger.infof("generate PSLG hmin:%f.03  hmax:%f.03", hmin, hmax);
		PSLGConverter pslgConverter = new PSLGConverter();
		PSLG pslg = pslgConverter.toPSLG(scenario.getTopography());
		File pslgFile = Paths.get(outDir.toString(), scenarioName + "-plsg.obj").toFile();

		MeshConstructor constructor = new MeshConstructor();
		// Floorfield Mesh
		logger.info("generate floor field mesh");
		var mesh = constructor.pslgToAdaptivePMesh(pslg, hmin, hmax, false);
		MeshPolyWriter<PVertex, PHalfEdge, PFace> meshPolyWriter = new MeshPolyWriter<>();
		String meshString = meshPolyWriter.to2DPoly(mesh);
		File meshFile = Paths.get(outDir.toString(), scenarioName + ".poly").toFile();
		writeFile(meshFile, meshString, (boolean) args.get("override"));

		// Background mesh
		logger.info("generate background mesh");
		var bMesh = constructor.pslgToCoarsePMesh(pslg, p -> Double.POSITIVE_INFINITY,false);
		meshString = meshPolyWriter.to2DPoly(mesh);
		meshFile = Paths.get(outDir.toString(), scenarioName + IOUtils.BACKGROUND_MESH_ENDING + ".poly").toFile();
		writeFile(meshFile, meshString, (boolean) args.get("override"));
	}

	private void writeFile(File file, String content, boolean override) throws IOException {
		if (!file.exists()  ||  override){
			file.createNewFile();
			try(FileWriter fd = new FileWriter(file)){
				fd.write(content);
				logger.infof("write %s", file.toString());
			}
		} else {
			logger.info("file already exists. (Override false)");
		}
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
