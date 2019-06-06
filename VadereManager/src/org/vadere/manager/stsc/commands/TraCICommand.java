package org.vadere.manager.stsc.commands;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.CmdType;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;
import org.vadere.manager.stsc.commands.control.TraCICloseCommand;
import org.vadere.manager.stsc.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.stsc.commands.control.TraCISimStepCommand;

import java.nio.ByteBuffer;

public abstract class TraCICommand {

	protected TraCICmd traCICmd;
	protected TraCIPacket NOK_response = null;

	public static TraCICommand create(ByteBuffer rawCmd){
		TraCICommandBuffer cmdBuffer = TraCICommandBuffer.wrap(rawCmd);

		int identifier = cmdBuffer.readCmdIdentifier();
		TraCICmd cmd = TraCICmd.fromId(identifier);

		switch (cmd.type){
			case CTRL:
				return createControlCommand(cmd, cmdBuffer);
			case VALUE_GET:
				return createGetCommand(cmd, cmdBuffer);
			case VALUE_SET:
				return createSetCommand(cmd, cmdBuffer);
			case VALUE_SUB:
			case CONTEXT_SUB:
				throw new TraCIException("Subscrtipons not implemente");
			case UNKNOWN:
				throw new TraCIException("Unknown command found found with identifier: " + identifier);
			default:
				throw new IllegalStateException("Should not be reached. All CmdType enums are tested in switch statement");
		}

	}

	private static TraCICommand createControlCommand(TraCICmd cmd, TraCICommandBuffer cmdBuffer){

		switch (cmd){
			case GET_VERSION:
				return new TraCIGetVersionCommand(cmd);
			case SIM_STEP:
				return new TraCISimStepCommand(cmd, cmdBuffer);
			case CLOSE:
				return new TraCICloseCommand(cmd);

			default:
				throw  new IllegalStateException("Should not be reached. Only TraCI control commands expected");
		}

	}

	private static TraCICommand createGetCommand(TraCICmd cmd, TraCICommandBuffer cmdBuffer){
		return new TraCIGetCommand(cmd, cmdBuffer);
	}

	private static TraCICommand createSetCommand(TraCICmd cmd, TraCICommandBuffer cmdBuffer){
		return new TraCISetCommand(cmd, cmdBuffer);
	}


	protected TraCICommand(TraCICmd traCICmd){
		this.traCICmd = traCICmd;
	}

	public TraCICmd getTraCICmd() {
		return traCICmd;
	}

	public CmdType getCmdType(){
		return traCICmd.type;
	}


	public TraCICommand setNOK_response(TraCIPacket NOK_response) {
		this.NOK_response = NOK_response;
		return this;
	}

	public abstract TraCIPacket buildResponsePacket();
}
