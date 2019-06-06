package org.vadere.manager.commandHandler;


import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.stsc.commands.control.TraCISimStepCommand;
import org.vadere.manager.stsc.respons.TraCIGetVersionResponse;
import org.vadere.manager.stsc.respons.TraCISimTimeResponse;

public class ControlCommandHandler extends CommandHandler{

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


}