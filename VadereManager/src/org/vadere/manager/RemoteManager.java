package org.vadere.manager;

import org.vadere.manager.commandHandler.StateAccessHandler;
import org.vadere.manager.commandHandler.Subscription;
import org.vadere.simulator.entrypoints.ScenarioFactory;
import org.vadere.simulator.projects.RunnableFinishedListener;
import org.vadere.simulator.projects.Scenario;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *  //todo comment
 */
public class RemoteManager implements RunnableFinishedListener {


	private static Logger logger = Logger.getLogger(RemoteManager.class);

	private RemoteScenarioRun currentSimulationRun;
	private Thread currentSimulationThread;
	private boolean simulationFinished;

	private List<Subscription> subscriptions;


	public RemoteManager() {
		subscriptions = new ArrayList<>();
	}

	public void loadScenario(String scenarioString) {
		Scenario scenario;
		try {
			scenario = ScenarioFactory.createScenarioWithScenarioJson(scenarioString);
		} catch (IOException e) {
			throw new TraCIException("Cannot create Scenario from given file.");
		}
		currentSimulationRun = new RemoteScenarioRun(scenario, this);
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

	@Override
	public void finished(Runnable runnable) {
		simulationFinished = true;
		logger.infof("Simulation finished.");
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

		logger.infof("Start Scenario %s with remote control...", currentSimulationRun.getScenario().getName());
		currentSimulationThread.start();
	}
}
