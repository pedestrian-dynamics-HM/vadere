package org.vadere.manager.stsc.commands.control;

import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;

import java.util.List;

public class TraCILoadCommand extends TraCICommand {

	private List<String> optionList;

	public static TraCIPacket build(List<String> optionList){
		TraCIPacket packet = TraCIPacket.create();
		return packet;
	}

	public TraCILoadCommand(TraCICommandBuffer cmdBuffer) {
		super(TraCICmd.LOAD);
		this.optionList = cmdBuffer.reader.readStringList();
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		return null;
	}
}
