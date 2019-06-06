package org.vadere.manager.commandHandler;


import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.stsc.commands.control.TraCISendFileCommand;
import org.vadere.manager.stsc.commands.control.TraCISimStepCommand;
import org.vadere.manager.stsc.respons.TraCIGetVersionResponse;
import org.vadere.manager.stsc.respons.TraCISimTimeResponse;
import org.vadere.simulator.projects.Scenario;
import org.vadere.util.logging.Logger;

public class ControlCommandHandler extends CommandHandler{

	private static Logger logger = Logger.getLogger(CommandExecutor.class);

	public static ControlCommandHandler instance;

	static {
		instance = new ControlCommandHandler();
	}

	private ControlCommandHandler(){}

	public TraCICommand process_load(TraCICommand rawCmd) {
		return null;
	}

	public TraCICommand process_close(TraCICommand rawCmd) {
		return null;
	}

	public TraCICommand process_simStep(TraCICommand rawCmd) {
		TraCISimStepCommand cmd = (TraCISimStepCommand) rawCmd;
		Object data = null; // handle subscriptions

		cmd.setResponse(new TraCISimTimeResponse(data));

		return cmd;
	}

	public TraCICommand process_getVersion(TraCICommand rawCmd) {

		TraCIGetVersionCommand cmd = (TraCIGetVersionCommand)rawCmd;
		cmd.setResponse(new TraCIGetVersionResponse(33, "Version 33 From Vadere"));

		return cmd;

	}


	public TraCICommand process_load_file(TraCICommand rawCmd) {

		TraCISendFileCommand cmd = (TraCISendFileCommand) rawCmd;

		Scenario s = new Scenario(cmd.getFile());

		logger.infof("Received scenario File: %s", s.getName());


		return cmd;
	}
}