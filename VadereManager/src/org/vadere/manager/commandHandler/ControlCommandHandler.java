package org.vadere.manager.commandHandler;


import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.stsc.commands.control.TraCISendFileCommand;
import org.vadere.manager.stsc.commands.control.TraCISimStepCommand;
import org.vadere.manager.stsc.respons.TraCIGetVersionResponse;
import org.vadere.manager.stsc.respons.TraCISimTimeResponse;
import org.vadere.util.logging.Logger;

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
		return null;
	}

	public TraCICommand process_simStep(TraCICommand rawCmd, RemoteManager remoteManager) {
		TraCISimStepCommand cmd = (TraCISimStepCommand) rawCmd;

		remoteManager.nextStep(cmd.getTargetTime());

		remoteManager.accessState(state -> {
			cmd.setResponse(new TraCISimTimeResponse(state.getStep(), TraCIDataType.INTEGER));
		});


		logger.infof("Simulate next step %f", cmd.getTargetTime());

		return cmd;
	}

	public TraCICommand process_getVersion(TraCICommand rawCmd, RemoteManager remoteManager) {

		TraCIGetVersionCommand cmd = (TraCIGetVersionCommand)rawCmd;
		cmd.setResponse(new TraCIGetVersionResponse(33, "Version 33 From Vadere"));

		return cmd;

	}


	public TraCICommand process_load_file(TraCICommand rawCmd, RemoteManager remoteManager) {

		TraCISendFileCommand cmd = (TraCISendFileCommand) rawCmd;

		remoteManager.loadScenario(cmd.getFile());
		remoteManager.run();

		return cmd;
	}
}