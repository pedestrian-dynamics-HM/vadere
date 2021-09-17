package org.vadere.manager;

import org.apache.commons.collections.CollectionUtils;
import org.vadere.gui.onlinevisualization.OnlineVisualization;
import org.vadere.manager.traci.commandHandler.StateAccessHandler;
import org.vadere.manager.traci.compound.object.SimulationCfg;
import org.vadere.simulator.control.simulation.SimThreadState;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.projects.RunnableFinishedListener;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.traci.TraCIException;
import org.vadere.state.traci.TraCIExceptionInternal;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;

/**
 * This class acts as interface between the TraCI handling and the actual simulation. All
 * synchronization is handled by the {@link RemoteScenarioRun} class. All access to the simulation
 * state must be wrapped within a {@link StateAccessHandler} to decouple the {@link SimulationState}
 * from the command handing.
 *
 * Within the {@link StateAccessHandler#execute(RemoteManager, SimulationState)} method the {@link
 * SimulationState} is save to access and change. Be really careful what you change!
 */
public class RemoteManager implements RunnableFinishedListener {


	private static Logger logger = Logger.getLogger(RemoteManager.class);

	private RemoteScenarioRun currentSimulationRun;
	private Thread currentSimulationThread;
	private boolean simulationFinished;
	private boolean clientCloseCommandReceived;
	private Path defaultOutputdir;    // defined by command line parameter. May be overwritten by simCfg
	private boolean guiSupport;
	private SimulationCfg simCfg;    // received from traci client.

	private List<Subscription> subscriptions;


	public RemoteManager(Path defaultOutputdir, boolean guiSupport) {
		this.defaultOutputdir = defaultOutputdir;
		this.guiSupport = guiSupport;
		this.subscriptions = new ArrayList<>();
		this.clientCloseCommandReceived = false;
		this.simCfg = null;
	}

	public void loadScenario(String scenarioString, Map<String, ByteArrayInputStream> cacheData) {

		Scenario scenario;
		ScenarioCache scenarioCache;
		Path scenarioPath;
		Path outputDir;
		if (simCfg != null) {
			outputDir = Paths.get(simCfg.outputPath());
			logger.infof("received output directory from traci client '%s'", simCfg.outputPath());
		} else {
			outputDir = defaultOutputdir;
		}

		try {
			scenario = ScenarioFactory.createScenarioWithScenarioJson(scenarioString);
			scenarioPath = defaultOutputdir.resolve(IOUtils.SCENARIO_DIR).resolve(scenario.getName()
					+ IOUtils.SCENARIO_FILE_EXTENSION);
			scenarioCache = buildScenarioCache(scenario, cacheData);
		} catch (IOException e) {
			throw new TraCIException("Cannot create Scenario from given file.");
		}
		if (simCfg != null) {
			if (simCfg.isUseVadereSeed()){
				logger.infof("use Vadere Seed. (ignoring traci seed)", Long.toString(simCfg.getSeed()));
			} else {
				scenario.getAttributesSimulation().setFixedSeed(simCfg.getSeed());
				scenario.getAttributesSimulation().setUseFixedSeed(true);
				logger.infof("received seed from traci client '%s'", Long.toString(simCfg.getSeed()));
			}
		}
		currentSimulationRun = new RemoteScenarioRun(scenario, outputDir, this, scenarioPath, scenarioCache);
	}

	public SimThreadState getCurrentSimThreadState(){
		return currentSimulationRun.getCurrentSimThreadState();
	}

	public void loadScenario(String scenarioString) {
		loadScenario(scenarioString, null);
	}

	private ScenarioCache buildScenarioCache(final Scenario scenario, Map<String, ByteArrayInputStream> cacheData) {
		ScenarioCache scenarioCache = ScenarioCache.load(scenario, defaultOutputdir);
		if (scenarioCache.isEmpty()) {
			if (cacheData != null) {
				logger.warnf("received cache data but given Scenario has cache deactivated. Received cache will be ignored");
				cacheData.clear();
			}
			return scenarioCache;
		}


		if (cacheData != null) {
			logger.info("received cache data");
			cacheData.forEach(scenarioCache::addReadOnlyCache);
		}

		return scenarioCache;
	}

	public boolean stopSimulationIfRunning(){
		if (currentSimulationThread != null && currentSimulationThread.isAlive()) {
			logger.errorf("kill simulation thread");
			currentSimulationThread.interrupt();
			return true;
		}

		return false;
	}

	public void addValueSubscription(Subscription sub) {
		subscriptions.add(sub);
	}

	public List<Subscription> getSubscriptions() {
		return subscriptions;
	}

	public boolean accessState(StateAccessHandler stateAccessHandler) {
		if (currentSimulationRun == null)
			return false;
		currentSimulationRun.accessState(this, stateAccessHandler);

		return true;
	}

	public RemoteScenarioRun getRemoteSimulationRun() {
		return currentSimulationRun;
	}


	public boolean nextStep(double simTime) {
		if (simulationFinished)
			return false;

		currentSimulationRun.nextStep(simTime);
		return true;
	}

	public void notifySimulationThread(){
		currentSimulationRun.notifySimulationThread();
	}

	public void waitForSimulationEnd(){
		currentSimulationRun.waitForSimulationEnd();
	}

	public double getSimulationStoppedEarlyAtTime(){
		return currentSimulationRun.getSimulationStoppedEarlyAtTime();
	}

	public boolean isClientCloseCommandReceived() {
		return clientCloseCommandReceived;
	}

	public void setClientCloseCommandReceived(boolean clientCloseCommandReceived) {
		this.clientCloseCommandReceived = clientCloseCommandReceived;
	}

	@Override
	public void finished(Runnable runnable) {
		simulationFinished = true;
		logger.infof("Simulation finished.");
		if (guiSupport)
			ServerView.close();
	}

	public void startSimulation() {
		if (currentSimulationRun == null)
			throw new TraCIExceptionInternal("RemoteScenarioRun object must not be null");

		if (currentSimulationThread != null && currentSimulationThread.isAlive())
			throw new TraCIExceptionInternal("A simulation is already running. Stop current simulation before starting new one.");

		simulationFinished = false;
		currentSimulationThread = new Thread(currentSimulationRun);
		currentSimulationThread.setUncaughtExceptionHandler((t, ex) -> {
			currentSimulationRun.simulationFailed(ex);
		});

		if (guiSupport) {
			OnlineVisualization onlineVisualization = new OnlineVisualization(true);
			currentSimulationRun.addPassiveCallback(onlineVisualization);
			ServerView.startServerGui(onlineVisualization);
		}

		logger.infof("Start Scenario %s with remote control...", currentSimulationRun.getScenario().getName());
		currentSimulationThread.start();
	}

	public SimulationCfg getSimCfg() {
		return simCfg;
	}

	public void setSimCfg(SimulationCfg simCfg) {
		this.simCfg = simCfg;
	}

	public Path getOutputDirectory(){
		return this.defaultOutputdir;
	}


}
