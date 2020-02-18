package org.vadere.manager;

import org.vadere.manager.traci.commandHandler.StateAccessHandler;
import org.vadere.simulator.control.simulation.RemoteRunListener;
import org.vadere.simulator.control.simulation.ScenarioRun;
import org.vadere.simulator.projects.RunnableFinishedListener;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.cache.ScenarioCache;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class RemoteScenarioRun extends ScenarioRun implements RemoteRunListener {

	private final Object waitForLoopEnd;
	private final ReentrantLock lock;
	private List<Subscription> subscriptions;
	private boolean lastSimulationStep;


	public RemoteScenarioRun(Scenario scenario, Path outputDir, RunnableFinishedListener scenarioFinishedListener, Path scenarioPath, ScenarioCache scenarioCache) {
		super(scenario, outputDir.toString(), scenarioFinishedListener, scenarioPath, scenarioCache);
		this.singleStepMode = true;
		this.waitForLoopEnd = new Object();
		this.lock = new ReentrantLock();
		this.lastSimulationStep = false;
		addRemoteManagerListener(this);
	}

	synchronized public boolean accessState(RemoteManager remoteManager, StateAccessHandler stateAccessHandler) {
		try {
			if (!isWaitForSimCommand()) {
				synchronized (waitForLoopEnd) {
					waitForLoopEnd.wait();
				}
			}
			lock.lock();
			stateAccessHandler.execute(remoteManager, getSimulationState());

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TraCIException("Interrupted while accessing simulation state");
		} finally {
			lock.unlock();
		}
		return true;
	}

	synchronized public void nextStep(double simTime) {
		try {
			lock.lock();
			nextSimCommand(simTime);

		} finally {
			lock.unlock();
		}
	}

	@Override
	public void simulationStepFinishedListener() {
		synchronized (waitForLoopEnd) {
			waitForLoopEnd.notify();
		}
	}

	@Override
	public void lastSimulationStepFinishedListener() {
		synchronized (waitForLoopEnd) {
			lastSimulationStep = true;
			waitForLoopEnd.notify();
		}
	}

	public boolean isLastSimulationStep() {
		return lastSimulationStep;
	}

}
