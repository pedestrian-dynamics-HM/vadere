package org.vadere.simulator.control.simulation;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.utils.io.poly.MeshPolyReader;
import org.vadere.meshing.utils.io.poly.MeshPolyWriter;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.control.psychology.cognition.helpers.CognitionModelBuilder;
import org.vadere.simulator.control.psychology.cognition.models.ICognitionModel;
import org.vadere.simulator.control.psychology.perception.models.IPerceptionModel;
import org.vadere.simulator.control.psychology.perception.helpers.PerceptionModelBuilder;
import org.vadere.simulator.control.psychology.perception.StimulusController;
import org.vadere.simulator.control.scenarioelements.TargetChangerController;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.MainModelBuilder;
import org.vadere.simulator.models.infection.AirTransmissionModel;
import org.vadere.simulator.models.potential.solver.EikonalSolverCacheProvider;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.projects.RunnableFinishedListener;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Manages single simulation runs.
 *
 * @author Jakob Schöttl
 *
 */
public class ScenarioRun implements Runnable {

	protected static Logger logger = Logger.getLogger(ScenarioRun.class);

	protected final ScenarioCache scenarioCache;

	protected Path outputPath;

	protected final Path scenarioFilePath;

	protected final List<PassiveCallback> passiveCallbacks = new LinkedList<>();

	protected final List<RemoteRunListener> remoteRunListeners = new ArrayList<>();

	protected final DataProcessingJsonManager dataProcessingJsonManager;

	protected Simulation simulation;

	protected boolean singleStepMode = false;

	// the processor is null if no output is written i.e. if scenarioStore.attributesSimulation.isWriteSimulationData() is false.
	protected @Nullable
	ProcessorManager processorManager;

	protected final Scenario scenario;
	protected final ScenarioStore scenarioStore; // contained in scenario, but here for convenience

	protected final RunnableFinishedListener finishedListener;

	protected SimulationResult simulationResult;

	public ScenarioRun(final Scenario scenario, RunnableFinishedListener scenarioFinishedListener, Path scenarioFilePath, boolean singleStepMode, ScenarioCache scenarioCache) {
		this(scenario, IOUtils.OUTPUT_DIR, scenarioFinishedListener, scenarioFilePath, scenarioCache);
		this.singleStepMode = singleStepMode;
	}

	public ScenarioRun(final Scenario scenario, RunnableFinishedListener scenarioFinishedListener, Path scenarioFilePath, ScenarioCache scenarioCache) {
		this(scenario, IOUtils.OUTPUT_DIR, scenarioFinishedListener, scenarioFilePath, scenarioCache);
	}

	public ScenarioRun(final Scenario scenario, final String outputDir, final RunnableFinishedListener scenarioFinishedListener, Path scenarioFilePath, ScenarioCache scenarioCache) {
		this(scenario, outputDir, false, scenarioFinishedListener, scenarioFilePath, scenarioCache);
	}

	// if overwriteTimestampSetting is true do note use timestamp in output directory
	public ScenarioRun(final Scenario scenario, final String outputDir, boolean overwriteTimestampSetting, final RunnableFinishedListener scenarioFinishedListener, Path scenarioFilePath, ScenarioCache scenarioCache) {
		this.scenario = scenario;
		this.scenario.setSimulationRunning(true); // create copy of ScenarioStore and redirect getScenarioStore to this copy for simulation.
		this.scenarioStore = scenario.getScenarioStore();
		this.dataProcessingJsonManager = scenario.getDataProcessingJsonManager();
		this.setOutputPaths(Paths.get(outputDir), overwriteTimestampSetting); // TODO [priority=high] [task=bugfix] [Error?] this is a relative path. If you start the application via eclipse this will be VadereParent/output
		this.finishedListener = scenarioFinishedListener;
		this.simulationResult = new SimulationResult(scenario.getName());
		this.scenarioFilePath = scenarioFilePath;
		this.scenarioCache = scenarioCache;
	}


	private void initializeVadereContext(){
		String scenarioName = scenario.getName();
		this.scenarioStore.getTopography().setContextId(scenarioName);
		VadereContext ctx = new VadereContext();

		if (scenarioCache.isNotEmpty())
			ctx.setEikonalSolverProvider(new EikonalSolverCacheProvider(scenarioCache)); // cache found use CacheProvider if possible

		ctx.put("cache", scenarioCache);
		ctx.put(AirTransmissionModel.simStepLength, scenario.getAttributesSimulation().getSimTimeStepLength());

		VadereContext.add(scenarioName, ctx);
		logger.info("scenario context initialized.");
	}

	/**
	 * This method runs a simulation. It must not catch any exceptions! The
	 * caller (i.e. the calling thread) should catch exceptions and call
	 * {@link #simulationFailed(Throwable)}.
	 */
	@Override
	public void run() {
		try {
			simulationResult.startTime();

			/**
			 * To make sure that no other Thread changes the scenarioStore object during the initialization of a scenario run
			 * this is an atomic operation with respect to the scenarioStore. We observed that with Linux 18.04 KUbunto
			 * the GUI-Thread changes the scenarioStore object during a simulation run. Which can lead to any unexpected behaviour.
			 */
			synchronized (scenarioStore) {
				logger.info(String.format("Initializing scenario: %s...", scenario.getName()));

				scenarioStore.getTopography().reset();
				initializeVadereContext();

				AMesh floorFieldMesh = loadFloorFieldMesh().orElse(null);
				AMesh backgroundMesh = loadBackgrounddMesh().orElse(null);
				MainModelBuilder modelBuilder = new MainModelBuilder(scenarioStore, floorFieldMesh, backgroundMesh);
				modelBuilder.createModelAndRandom();

				final MainModel mainModel = modelBuilder.getModel();
				final Random random = modelBuilder.getRandom();
				final Domain domain = modelBuilder.getDomain();
				//todo[random]: place the Random object in the context for now. This should be replaced by the meta seed.
				VadereContext.getCtx(scenarioStore.getTopography()).put("random", random);

				// prepare processors and simulation data writer
				if(scenarioStore.getAttributesSimulation().isWriteSimulationData()) {
					processorManager = dataProcessingJsonManager.createProcessorManager(mainModel, scenarioStore.getTopography());
					processorManager.setSimulationResult(simulationResult);
				}

				// Only create output directory and write .scenario file if there is any output.
				if (processorManager != null && !processorManager.isEmpty()) {
					createAndSetOutputDirectory();
					scenario.saveToOutputPath(outputPath);
					if(floorFieldMesh != null) {
						writeFloorFieldMeshToOutput(floorFieldMesh);
					}
					if(backgroundMesh != null) {
						writeBackgroundMeshToOutput(backgroundMesh);
					}
				}

				IPerceptionModel perceptionModel = PerceptionModelBuilder.instantiateModel(scenarioStore);
				ICognitionModel cognitionModel = CognitionModelBuilder.instantiateModel(scenarioStore);


				// ensure all elements have unique id before attributes are sealed
				scenario.getTopography().generateUniqueIdIfNotSet();
				sealAllAttributes();

				// Run simulation main loop from start time = 0 seconds
				simulation = new Simulation(mainModel, perceptionModel,
						cognitionModel, 0.0,
						scenarioStore.getName(), scenarioStore, domain,
						passiveCallbacks, random,
						processorManager, simulationResult,
						remoteRunListeners, singleStepMode,
						scenarioCache);
			}

			simulation.run();
			simulationResult.setState("SimulationRun completed");

		} catch (Exception e) {
			logger.error("Simulation failed", e);
			throw new RuntimeException("Simulation failed.", e);
		} finally {
			simulationResult.stopTime();
			logger.info("Simulation run finished.");
			doAfterSimulation();
			VadereContext.remove(scenarioStore.getTopography().getContextId());

		}
	}

	private Optional<AMesh> loadFloorFieldMesh() {
		return loadMesh(scenario.getName()+".poly");
	}

	private Optional<AMesh> loadBackgrounddMesh() {
		return loadMesh(scenario.getName()+IOUtils.BACKGROUND_MESH_ENDING+".poly");
	}

	private Optional<AMesh> loadMesh(@NotNull final String fileName) {
		AMesh mesh = null;
		try {
			var meshReader = new MeshPolyReader<>(() -> new AMesh());
			Path path = scenarioFilePath.getParent().resolve(IOUtils.MESH_DIR + "/" + fileName);
			mesh = (AMesh) meshReader.readMesh(new FastBufferedInputStream(new FileInputStream(path.toFile())));
		} catch (FileNotFoundException e) {
			logger.info("no mesh " + fileName + " was found.");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Optional.ofNullable(mesh);
	}

	private void writeFloorFieldMeshToOutput(@NotNull final AMesh mesh) {
		writeMeshToOutput(outputPath, scenario.getName()+".poly", mesh);
	}

	private void writeBackgroundMeshToOutput(@NotNull final AMesh mesh) {
		writeMeshToOutput(outputPath, scenario.getName()+IOUtils.BACKGROUND_MESH_ENDING+".poly", mesh);
	}

	private void writeMeshToOutput(@NotNull final Path output, @NotNull final String fileName, @NotNull final AMesh mesh) {
		try (PrintWriter out = new PrintWriter(Paths.get(output.toString(), fileName).toString())) {
			MeshPolyWriter<AVertex, AHalfEdge, AFace> meshWriter = new MeshPolyWriter<>();
			meshWriter.to2DPoly(mesh, 0, null, v -> false, out);
			logger.info("write mesh to " + fileName + ".");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void simulationFailed(Throwable e) {
		e.printStackTrace();
		logger.error(e);
	}

	protected void doAfterSimulation() {
		if (finishedListener != null)
			finishedListener.finished(this);

		scenario.setSimulationRunning(false); // remove  simulation copy of ScenarioStore and redirect getScenarioStore to base copy.
		logger.info(String.format("Simulation of scenario %s finished.", scenario.getName()));
	}

	public boolean isRunning() {
		return simulation != null && simulation.isRunning();
	}

	public void setIsRunSimulation(boolean running) {
		if (simulation != null) {
			simulation.setIsRunSimulation(running);
		}
	}
	

	public boolean isScenarioInSingleStepMode(){
		return simulation != null && simulation.isSingleStepMode();
	}

	public void setSingleStepMode(boolean singleStepMode){

		simulation.setSingleStepMode(singleStepMode);
	}

	public boolean isWaitForSimCommand(){
		return simulation != null && simulation.isWaitForSimCommand();
	}

	public boolean checkValidThreadState(){
		if (simulation == null){
			return false;
		}
		return SimThreadState.MAIN_LOOP.equals(simulation.getThreadState()) || SimThreadState.POST_LOOP.equals(simulation.getThreadState());
	}

	public void nextSimCommand(double simulateUntilInSec){

		if ( !simulation.isSingleStepMode())
			throw new IllegalStateException("Simulation is not in 'remoteControl' state");

		simulation.nextSimCommand(simulateUntilInSec);
	}

	public void addTargetChangerController(TargetChangerController controller){
		simulation.addTargetChangerController(controller);
	}

	public void addStimulusInfo(StimulusInfo si){
		simulation.addStimulusInfo(si);
	}

	public void addTargetChangeController(TargetChangerController controller){
		simulation.addTargetChangerController(controller);
	}

	public void pause() {

		if(! simulation.isRunning())
			throw new IllegalStateException("Received trigger to pause the simulation, but it is not running!");

		if (simulation != null)
			simulation.pause();
	}

	public void resume() {

		if(simulation.isRunning())
			throw new IllegalStateException("Received trigger to resume the simulation, but it is not paused!");

		if (simulation != null)
			simulation.resume();
	}

	// only allow subclasses access to the not final state.
	// this is needed to allow vadere to receive SET-Commands.
	protected SimulationState getSimulationState(){
		return simulation.getSimulationState();
	}

	public void addPassiveCallback(final PassiveCallback pc) {
		passiveCallbacks.add(pc);
	}

	public void addRemoteManagerListener(final RemoteRunListener listener){
		remoteRunListeners.add(listener);
	}

	public boolean isSingleStepMode() {
		return singleStepMode;
	}

	public void setOutputPaths(final Path outputPath, boolean overwriteTimestampSetting){
		if (overwriteTimestampSetting){
			this.outputPath = outputPath;
		} else {
			setOutputPaths(outputPath);
		}
	}

	public void setOutputPaths(final Path outputPath) {
		if (dataProcessingJsonManager.isTimestamped()) {
			String dateString = new SimpleDateFormat(IOUtils.DATE_FORMAT).format(new Date());
			this.outputPath = Paths.get(outputPath.toString(), String.format("%s_%s", scenario.getName(), dateString));
		} else {
			this.outputPath = Paths.get(outputPath.toString(), scenario.getName());
		}
	}

	public Path getOutputPath() {
		return Paths.get(this.outputPath.toString());
	}

	private void createAndSetOutputDirectory() {
		try {
			// Create output directory
			Files.createDirectories(outputPath);
			processorManager.setOutputFiles(outputPath.toString());
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public String toString() {
		return scenario.getName();
	}

	@Deprecated  // Deprecated, because it is currently not used in code anymore
	public String readyToRunResponse() { // TODO [priority=medium] [task=check] add more conditions
		if (scenarioStore.getMainModel() == null) {
			return scenarioStore.getName() + ": no mainModel is set";
		}
		return null;
	}

	public Scenario getScenario() {
		return scenario;
	}

	public StimulusController getStimulusController() {
		return simulation.getStimulusController();
	}

	public SimulationResult getSimulationResult() {
		return simulationResult;
	}

	private void sealAllAttributes() {
		scenarioStore.sealAllAttributes();

		if (processorManager != null) {
			processorManager.sealAllAttributes();
		}
	}



}
