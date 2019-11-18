package org.vadere.manager;

import org.vadere.gui.onlinevisualization.OnlineVisualization;
import org.vadere.manager.traci.commandHandler.StateAccessHandler;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.projects.RunnableFinishedListener;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  This class acts as interface between the TraCI handling and the actual simulation.
 *  All synchronization is handled by the {@link RemoteScenarioRun} class. All access to
 *  the simulation state must be wrapped within a {@link StateAccessHandler} to decouple
 *  the {@link SimulationState} from the command handing.
 *
 *  Within the {@link StateAccessHandler#execute(RemoteManager, SimulationState)} method
 *  the {@link SimulationState} is save to access and change. Be really careful what you
 *  change!
 *
 */
public class RemoteManager implements RunnableFinishedListener {


	private static Logger logger = Logger.getLogger(RemoteManager.class);

	private RemoteScenarioRun currentSimulationRun;
	private Thread currentSimulationThread;
	private boolean simulationFinished;
	private boolean clientCloseCommandReceived;
	private Path baseDir;
	private boolean guiSupport;

	private List<Subscription> subscriptions;


	public RemoteManager(Path baseDir, boolean guiSupport) {
		this.baseDir = baseDir;
		this.guiSupport = guiSupport;
		this.subscriptions = new ArrayList<>();
		this.clientCloseCommandReceived = false;
	}

	public void loadScenario(String scenarioString, Map<String, ByteArrayInputStream> cacheData){

		Scenario scenario;
		ScenarioCache scenarioCache;
		Path scenarioPath;
		Path outputDir;
		try {
			scenario = ScenarioFactory.createScenarioWithScenarioJson(scenarioString);
			scenarioPath = baseDir.resolve(IOUtils.SCENARIO_DIR).resolve(scenario.getName() + IOUtils.SCENARIO_FILE_EXTENSION);
			scenarioCache = buildScenarioCache(scenario, cacheData);
		} catch (IOException e) {
			throw new TraCIException("Cannot create Scenario from given file.");
		}
		currentSimulationRun = new RemoteScenarioRun(scenario, baseDir,this, scenarioPath, scenarioCache);
	}

	public void loadScenario(String scenarioString) {

		loadScenario(scenarioString, null);
	}

	private ScenarioCache buildScenarioCache(final Scenario scenario, Map<String, ByteArrayInputStream> cacheData){
		ScenarioCache scenarioCache = ScenarioCache.load(scenario, baseDir);
		if (scenarioCache.isEmpty()){
			if (cacheData != null){
				logger.warnf("received cache data but given Scenario has cache deactivated. Received cache will be ignored");
				cacheData.clear();
			}
			return scenarioCache;
		}


		if (cacheData != null){
			logger.info("received cache data");
			cacheData.forEach(scenarioCache::addReadOnlyCache);
		}

		return scenarioCache;
	}

	public boolean stopSimulationIfRunning(){
		if (currentSimulationThread != null && currentSimulationThread.isAlive()){
			currentSimulationThread.interrupt();
			return true;
		}

		return false;
	}

	public void addValueSubscription(Subscription sub){
		subscriptions.add(sub);
	}

	public List<Subscription> getSubscriptions(){
		return subscriptions;
	}

	public boolean accessState(StateAccessHandler stateAccessHandler){
		if (currentSimulationRun == null)
			return false;

		currentSimulationRun.accessState(this, stateAccessHandler);

		return true;
	}

	public boolean nextStep(double simTime){
		if (simulationFinished)
			return false;

		currentSimulationRun.nextStep(simTime);
		return true;
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

	public void startSimulation(){
		if (currentSimulationRun == null)
			throw new IllegalStateException("RemoteScenarioRun object must not be null");

		if (currentSimulationThread != null && currentSimulationThread.isAlive())
			throw  new IllegalStateException("A simulation is already running. Stop current simulation before starting new one.");

		simulationFinished = false;
		currentSimulationThread = new Thread(currentSimulationRun);
		currentSimulationThread.setUncaughtExceptionHandler((t, ex) -> {
			currentSimulationRun.simulationFailed(ex);
		});

		if (guiSupport){
			OnlineVisualization onlineVisualization = new OnlineVisualization(true);
			currentSimulationRun.addPassiveCallback(onlineVisualization);
			ServerView.startServerGui(onlineVisualization);
		}

		logger.infof("Start Scenario %s with remote control...", currentSimulationRun.getScenario().getName());
		currentSimulationThread.start();
	}
}
