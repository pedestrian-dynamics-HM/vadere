package org.vadere.manager;

import org.vadere.simulator.control.RemoteManagerListener;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.projects.RunnableFinishedListener;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;
import org.vadere.util.logging.Logger;

import java.io.IOException;

public class RemoteManager implements RemoteManagerListener, RunnableFinishedListener, Runnable {


	private static Logger logger = Logger.getLogger(RemoteManager.class);

	private ScenarioRun currentSimulationRun;
	private Thread currentSimulationThread;


	RemoteManager() { }

	public void loadScenario(String scenarioString) throws IOException {
		Scenario scenario = ScenarioFactory.createScenarioWithScenarioJson(scenarioString);
		currentSimulationRun = new ScenarioRun(scenario, this);
		currentSimulationRun.addRemoteManagerListener(this);
		currentSimulationRun.setSingleStepMode(true);
	}

	@Override
	public void simulationStepFinishedListener() {

	}

	@Override
	public void finished(Runnable runnable) {
		logger.infof("Simulation finished.");
	}

	@Override
	public void run() {
		startSimulation();
	}

	private void startSimulation(){
		if (currentSimulationRun == null)
			throw new IllegalStateException("ScenarioRun object must not be null");

		currentSimulationThread = new Thread(currentSimulationRun);
		currentSimulationThread.setUncaughtExceptionHandler((t, ex) -> {
			currentSimulationRun.simulationFailed(ex);
		});

		logger.infof("Start Scenario %s with remote control...", currentSimulationRun.getScenario().getName());
		currentSimulationThread.start();
	}
}
