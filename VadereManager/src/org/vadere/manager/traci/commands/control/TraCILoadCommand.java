package org.vadere.manager.traci.commands.control;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.reader.TraCICommandBuffer;

import java.util.List;

public class TraCILoadCommand extends TraCICommand {

	private List<String> optionList;

	public static TraCIPacket build(List<String> optionList){
		TraCIPacket packet = TraCIPacket.create();
		return packet;
	}

	public TraCILoadCommand(TraCICommandBuffer cmdBuffer) {
		super(TraCICmd.LOAD);
		this.optionList = cmdBuffer.readStringList();
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		return null;
	}
}
