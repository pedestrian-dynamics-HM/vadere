package org.vadere.manager.commandHandler;


import org.vadere.manager.RemoteManager;
import org.vadere.manager.VadereServer;
import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.control.TraCICloseCommand;
import org.vadere.manager.stsc.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.stsc.commands.control.TraCISendFileCommand;
import org.vadere.manager.stsc.commands.control.TraCISimStepCommand;
import org.vadere.manager.stsc.respons.TraCIGetVersionResponse;
import org.vadere.manager.stsc.respons.TraCISimTimeResponse;
import org.vadere.util.logging.Logger;

/**
 * Handel {@link org.vadere.manager.stsc.commands.TraCICommand}s for the Control API
 */
public class ControlCommandHandler extends CommandHandler{

	private static Logger logger = Logger.getLogger(CommandExecutor.class);

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

		remoteManager.nextStep(cmd.getTargetTime());

		remoteManager.accessState((manger, state) -> {
			cmd.setResponse(new TraCISimTimeResponse(state.getStep(), TraCIDataType.INTEGER));
		});

		logger.infof("Simulate next step %f", cmd.getTargetTime());

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
		remoteManager.run();

		return cmd;
	}
}