package org.vadere.manager;

import org.vadere.manager.commandHandler.StateAccessHandler;
import org.vadere.manager.commandHandler.Subscription;
import org.vadere.simulator.control.RemoteRunListener;
import org.vadere.simulator.projects.RunnableFinishedListener;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.control.ScenarioRun;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class RemoteScenarioRun extends ScenarioRun implements RemoteRunListener {

	private List<Subscription> subscriptions;
	private final Object waitForLoopEnd;
	private final ReentrantLock lock;
	private boolean lastSimulationStep;


	public RemoteScenarioRun(Scenario scenario, RunnableFinishedListener scenarioFinishedListener) {
		super(scenario, scenarioFinishedListener);
		this.singleStepMode = true;
		this.waitForLoopEnd = new Object();
		this.lock = new ReentrantLock();
		this.lastSimulationStep = false;
		addRemoteManagerListener(this);
	}

	synchronized public boolean accessState(RemoteManager remoteManager, StateAccessHandler stateAccessHandler){
		try {
			if (!isWaitForSimCommand()) {
				synchronized (waitForLoopEnd){
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

	synchronized public void nextStep(double simTime){
		try {
			lock.lock();
			nextSimCommand(simTime);

		} finally {
			lock.unlock();
		}
	}

	@Override
	public void simulationStepFinishedListener() {
		synchronized (waitForLoopEnd){
			waitForLoopEnd.notify();
		}
	}

	@Override
	public void lastSimulationStepFinishedListener() {
		synchronized (waitForLoopEnd){
			lastSimulationStep = true;
			waitForLoopEnd.notify();
		}
	}

	public boolean isLastSimulationStep() {
		return lastSimulationStep;
	}

}
