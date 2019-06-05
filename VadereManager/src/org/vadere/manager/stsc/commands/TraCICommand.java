package org.vadere.manager.stsc.commands;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.TraCICommandBuffer;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.control.CmdClose;
import org.vadere.manager.stsc.commands.control.CmdGetVersion;
import org.vadere.manager.stsc.commands.control.CmdSimStep;

import java.nio.ByteBuffer;

public abstract class TraCICommand {

	protected TraCICmd traCICmd;
//	protected TraCICommandBuffer data;

	public static TraCICommand createCommand(ByteBuffer rawCmd){
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
				return new CmdGetVersion(cmd);
			case SIM_STEP:
				return new CmdSimStep(cmd, cmdBuffer);
			case CLOSE:
				return new CmdClose(cmd);

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


	public abstract TraCIPacket handleCommand(TraCIPacket response);

	public TraCICmd getTraCICmd() {
		return traCICmd;
	}

	public CmdType getCmdType(){
		return traCICmd.type;
	}

}
