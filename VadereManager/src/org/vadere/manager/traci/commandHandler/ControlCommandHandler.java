package org.vadere.manager.traci.commandHandler;


import org.vadere.manager.RemoteManager;
import org.vadere.manager.Subscription;
import org.vadere.manager.server.VadereServer;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.TraCIVersion;
import org.vadere.manager.traci.commandHandler.annotation.ControlHandler;
import org.vadere.manager.traci.commandHandler.annotation.ControlHandlers;
import org.vadere.manager.traci.commandHandler.variables.ControlVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.control.*;
import org.vadere.manager.traci.response.*;
import org.vadere.simulator.control.simulation.SimThreadState;
import org.vadere.util.logging.Logger;

import java.lang.reflect.Method;

/**
 * Handel {@link org.vadere.manager.traci.commands.TraCICommand}s for the Control API
 */
public class ControlCommandHandler extends CommandHandler<ControlVar> {

	public static ControlCommandHandler instance;
	private static Logger logger = Logger.getLogger(ControlCommandHandler.class);

	static {
		instance = new ControlCommandHandler();
	}

	private ControlCommandHandler() {
		super();
		init(ControlHandler.class, ControlHandlers.class);
	}

	@Override
	protected void init_HandlerSingle(Method m) {
		ControlHandler an = m.getAnnotation(ControlHandler.class);
		putHandler(an.cmd(), an.var(), m);
	}

	@Override
	protected void init_HandlerMult(Method m) {
		ControlHandler[] ans = m.getAnnotation(ControlHandlers.class).value();
		for (ControlHandler a : ans) {
			putHandler(a.cmd(), a.var(), m);
		}
	}

	public TraCICommand process_load(TraCICommand rawCmd, RemoteManager remoteManager) {
		return null;
	}

	public TraCICommand process_close(TraCICommand rawCmd, RemoteManager remoteManager) {

		TraCICloseCommand cmd = (TraCICloseCommand) rawCmd;

		remoteManager.setClientCloseCommandReceived(true);
		
		if (remoteManager.getCurrentSimThreadState().equals(SimThreadState.MAIN_LOOP)) {

			logger.info("Current simulation run in main loop. Stop simulation.");
			remoteManager.getRemoteSimulationRun().setIsRunSimulation(false); // is_running = false in Simulation!
			logger.info("Wake up simulation thread.");
			remoteManager.getRemoteSimulationRun().notifySimulationThread(); // wake up simulation thread
			logger.info("Wait for simulation end.");
			remoteManager.waitForSimulationEnd(); // wait for simulation thread
			logger.info("Wait for simulation end finished.");
		}


		if (remoteManager.stopSimulationIfRunning())
			cmd.getResponse().getStatusResponse().setDescription("Stop simulation waiting for client close EOF");
		else
			cmd.getResponse().getStatusResponse().setDescription("waiting for client close EOF");
		return cmd;
	}

	public TraCICommand process_getState(TraCICommand rawCmd, RemoteManager remoteManager){
		TraCIGetStateCommand cmd = (TraCIGetStateCommand) rawCmd;

		remoteManager.getSubscriptions().forEach(sub -> sub.executeSubscription(remoteManager));


		// get responses
		TraCIGetStateResponse response = new TraCIGetStateResponse(
				new StatusResponse(cmd.getTraCICmd(), TraCIStatusResponse.OK, ""));

		remoteManager.getSubscriptions().forEach(sub -> {
			response.addSubscriptionResponse(sub.getValueSubscriptionCommand().getResponse());
		});
		cmd.setResponse(response);

		return cmd;
	}

	public TraCICommand process_simStep(TraCICommand rawCmd, RemoteManager remoteManager) {
		TraCISimStepCommand cmd = (TraCISimStepCommand) rawCmd;

		logger.debugf("%s: Simulate until=%f", TraCICmd.SIM_STEP.name(), cmd.getTargetTime());
		if (!remoteManager.nextStep(cmd.getTargetTime())) {
			// Simulation finished. Wait until simulation thread finishes with post loop before
			// telling traci client
			// TODO: check if this is reachable
			if (remoteManager.getCurrentSimThreadState().equals(SimThreadState.MAIN_LOOP)){
				remoteManager.waitForSimulationEnd();
			}
			cmd.setResponse(TraCISimTimeResponse.simEndReached());
			remoteManager.notifySimulationThread();
			return cmd;
		}
		// execute all
		logger.debugf("%s: execute %d subscriptions",
				TraCICmd.SIM_STEP.name(),
				remoteManager.getSubscriptions().size());
		remoteManager.getSubscriptions().forEach(sub -> sub.executeSubscription(remoteManager));

		// remove subscriptions no longer valid
		remoteManager.getSubscriptions().removeIf(Subscription::isMarkedForRemoval);

		// get responses
		TraCISimTimeResponse response = new TraCISimTimeResponse(
				new StatusResponse(cmd.getTraCICmd(), TraCIStatusResponse.OK, ""));

		remoteManager.getSubscriptions().forEach(sub -> {
			response.addSubscriptionResponse(sub.getValueSubscriptionCommand().getResponse());
		});
		cmd.setResponse(response);

		// check RemoteManager if simulation is ended prematurely. This can happen if the
		// a finish state is reached earlier than the given simulation time. In this case
		// inform the TraCI client about this instead of giving it the Subscription results.
		if (remoteManager.getSimulationStoppedEarlyAtTime() != Double.MAX_VALUE){
			double stoppedAtTime = remoteManager.getSimulationStoppedEarlyAtTime();
			logger.infof("Stop simulation at %f. Inform TraCI client with simEndReach Response.", stoppedAtTime);
			// Simulation finished. Wait until simulation thread finishes with post loop before
			// telling traci client
			if (remoteManager.getCurrentSimThreadState().equals(SimThreadState.MAIN_LOOP)){

				logger.errorf("Main loop not finished.");
			}
			cmd.setResponse(TraCISimTimeResponse.simEndReached());
			logger.info("Sent simEndReached command to client.");

			remoteManager.notifySimulationThread();
			logger.info("Notified simulation thread");
		}

		logger.debug("process_simStep done.");
		return cmd;
	}

	public TraCICommand process_getVersion(TraCICommand rawCmd, RemoteManager remoteManager) {

		TraCIGetVersionCommand cmd = (TraCIGetVersionCommand) rawCmd;
		cmd.setResponse(new TraCIGetVersionResponse(VadereServer.currentVersion));

		return cmd;
	}

	public TraCICommand process_load_file(TraCICommand rawCmd, RemoteManager remoteManager) {

		if (VadereServer.currentVersion.greaterOrEqual(TraCIVersion.V20_0_2)) {
			TraCISendFileCommandV20_0_1 cmd = (TraCISendFileCommandV20_0_1) rawCmd;

			remoteManager.loadScenario(cmd.getFile(), cmd.getCacheData());
			remoteManager.startSimulation();
			return cmd;
		} else {
			TraCISendFileCommand cmd = (TraCISendFileCommand) rawCmd;

			remoteManager.loadScenario(cmd.getFile());
			remoteManager.startSimulation();
			return cmd;
		}
	}
}