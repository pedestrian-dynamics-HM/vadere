package org.vadere.manager;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.vadere.manager.traci.commandHandler.StateAccessHandler;
import org.vadere.simulator.control.simulation.RemoteRunListener;
import org.vadere.simulator.control.simulation.ScenarioRun;
import org.vadere.simulator.control.simulation.SimThreadState;
import org.vadere.simulator.projects.RunnableFinishedListener;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.state.traci.TraCIException;

public class RemoteScenarioRun extends ScenarioRun implements RemoteRunListener {

  private final Object waitForSimStepLoopEnd;
  private final ReentrantLock lock;
  private List<Subscription> subscriptions;
  private double simulationStoppedEarlyAtTime;

  public RemoteScenarioRun(
      Scenario scenario,
      Path outputDir,
      RunnableFinishedListener scenarioFinishedListener,
      Path scenarioPath,
      ScenarioCache scenarioCache) {
    // overwriteTimestampSetting. In RemoteScenarioRun the caller defines where the output should
    // go.
    super(
        scenario,
        outputDir.toString(),
        true,
        scenarioFinishedListener,
        scenarioPath,
        scenarioCache);
    this.singleStepMode = true;
    this.waitForSimStepLoopEnd = new Object();
    this.lock = new ReentrantLock();
    this.simulationStoppedEarlyAtTime = Double.MAX_VALUE;
    addRemoteManagerListener(this);
  }

  public synchronized boolean accessState(
      RemoteManager remoteManager, StateAccessHandler stateAccessHandler) {
    try {
      if (!isWaitForSimCommand()) {
        synchronized (waitForSimStepLoopEnd) {
          waitForSimStepLoopEnd.wait();
        }
      }
      lock.lock();
      if (!checkValidThreadState()) {
        throw new TraCIException(
            "Invalid access to simulation state. Simulation thread in state %s",
            simulation.getThreadState().name());
      }
      stateAccessHandler.execute(remoteManager, getSimulationState());

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TraCIException("Interrupted while accessing simulation state");
    } finally {
      lock.unlock();
    }
    return true;
  }

  public synchronized SimThreadState getCurrentSimThreadState() {
    if (simulation == null) {
      return SimThreadState.INIT;
    } else if (simulation.getThreadState() == null) {
      return SimThreadState.INIT;
    }
    return simulation.getThreadState();
  }

  public synchronized void waitForSimulationEnd() {
    try {
      synchronized (waitForSimStepLoopEnd) {
        waitForSimStepLoopEnd.wait();
      }

    } catch (InterruptedException e) {
      logger.errorf("Interrupted while waiting for simulation thread to finish post loop");
    }
  }

  public synchronized void nextStep(double simTime) {
    try {
      lock.lock();
      nextSimCommand(simTime);

    } finally {
      lock.unlock();
    }
  }

  public synchronized void notifySimulationThread() {
    nextStep(-1);
  }

  @Override
  public void notifySimStepListener() {
    synchronized (waitForSimStepLoopEnd) {
      waitForSimStepLoopEnd.notify();
    }
  }

  @Override
  public void notifySimulationEndListener() {
    synchronized (waitForSimStepLoopEnd) {
      waitForSimStepLoopEnd.notify();
    }
  }

  @Override
  public void simulationStoppedEarlyListener(double time) {
    simulationStoppedEarlyAtTime = time;
  }

  public double getSimulationStoppedEarlyAtTime() {
    return simulationStoppedEarlyAtTime;
  }
}
