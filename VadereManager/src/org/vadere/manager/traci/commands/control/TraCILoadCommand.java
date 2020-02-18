package org.vadere.manager.traci.commands.control;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.reader.TraCICommandBuffer;
import org.vadere.manager.traci.writer.TraCIPacket;

import java.util.List;

public class TraCILoadCommand extends TraCICommand {

	private List<String> optionList;

	public TraCILoadCommand(TraCICommandBuffer cmdBuffer) {
		super(TraCICmd.LOAD);
		this.optionList = cmdBuffer.readStringList();
	}

	public static TraCIPacket build(List<String> optionList) {
		return TraCIPacket.create();
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		return null;
	}
}
