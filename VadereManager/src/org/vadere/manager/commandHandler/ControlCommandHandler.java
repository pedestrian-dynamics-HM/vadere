package org.vadere.manager.commandHandler;


import org.vadere.manager.RemoteManager;
import org.vadere.manager.VadereServer;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.control.TraCICloseCommand;
import org.vadere.manager.stsc.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.stsc.commands.control.TraCISendFileCommand;
import org.vadere.manager.stsc.commands.control.TraCISimStepCommand;
import org.vadere.manager.stsc.respons.StatusResponse;
import org.vadere.manager.stsc.respons.TraCIGetVersionResponse;
import org.vadere.manager.stsc.respons.TraCISimTimeResponse;
import org.vadere.manager.stsc.respons.TraCIStatusResponse;
import org.vadere.util.logging.Logger;

/**
 * Handel {@link org.vadere.manager.stsc.commands.TraCICommand}s for the Control API
 */
public class ControlCommandHandler extends CommandHandler{

	private static Logger logger = Logger.getLogger(ControlCommandHandler.class);

	public static ControlCommandHandler instance;

	static {
		instance = new ControlCommandHandler();
	}

	private ControlCommandHandler(){}

	public TraCICommand process_load(TraCICommand rawCmd, RemoteManager remoteManager) {
		return null;
	}

	public TraCICommand process_close(TraCICommand rawCmd, RemoteManager remoteManager) {

		TraCICloseCommand cmd = (TraCICloseCommand)rawCmd;

		if (remoteManager.stopSimulationIfRunning())
			cmd.getResponse().getStatusResponse().setDescription("Stop simulation waiting for client close EOF");
		else
			cmd.getResponse().getStatusResponse().setDescription("waiting for client close EOF");

		return cmd;
	}

	public TraCICommand process_simStep(TraCICommand rawCmd, RemoteManager remoteManager) {
		TraCISimStepCommand cmd = (TraCISimStepCommand) rawCmd;

		logger.infof("Simulate to: %f", cmd.getTargetTime());
//		remoteManager.nextStep(cmd.getTargetTime());
		if (!remoteManager.nextStep(cmd.getTargetTime())) {
			//simulation finished;
			cmd.setResponse(TraCISimTimeResponse.simEndReached());
			return cmd;
		}
		// execute all
		logger.debug("execute subscriptions");
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

		logger.debug("process_simStep done.");
		return cmd;
	}

	public TraCICommand process_getVersion(TraCICommand rawCmd, RemoteManager remoteManager) {

		TraCIGetVersionCommand cmd = (TraCIGetVersionCommand)rawCmd;
		cmd.setResponse(new TraCIGetVersionResponse(VadereServer.SUPPORTED_TRACI_VERSION,
				VadereServer.SUPPORTED_TRACI_VERSION_STRING));

		return cmd;
	}

	public TraCICommand process_load_file(TraCICommand rawCmd, RemoteManager remoteManager) {

		TraCISendFileCommand cmd = (TraCISendFileCommand) rawCmd;

		remoteManager.loadScenario(cmd.getFile());
		remoteManager.startSimulation();

		return cmd;
	}
}