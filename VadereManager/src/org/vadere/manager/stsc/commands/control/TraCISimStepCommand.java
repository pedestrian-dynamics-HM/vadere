package org.vadere.manager.stsc.commands.control;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.TraCICommandBuffer;
import org.vadere.manager.stsc.commands.TraCICmd;
import org.vadere.manager.stsc.commands.TraCICommand;

public class TraCISimStepCommand extends TraCICommand {

	private double targetTime;

	public TraCISimStepCommand(TraCICmd traCICmd, TraCICommandBuffer cmdBuffer) {
		super(traCICmd);
		try{
			this.targetTime = cmdBuffer.reader.readDouble();
		} catch (Exception e){
			throw TraCIException.cmdErr(traCICmd, e);
		}
	}

	public double getTargetTime() {
		return targetTime;
	}

}
