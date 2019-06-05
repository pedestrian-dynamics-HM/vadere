package org.vadere.manager.stsc.commands.control;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.TraCICommandBuffer;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICmd;
import org.vadere.manager.stsc.commands.TraCICommand;

public class CmdSimStep extends TraCICommand {

	double targetTime;

	public CmdSimStep(TraCICmd traCICmd, TraCICommandBuffer cmdBuffer) {
		super(traCICmd);
		try{
			this.targetTime = cmdBuffer.reader.readDouble();
		} catch (Exception e){
			throw TraCIException.cmdErr(traCICmd, e);
		}
	}

	@Override
	public TraCIPacket handleCommand(TraCIPacket response) {
		return null;
	}
}
