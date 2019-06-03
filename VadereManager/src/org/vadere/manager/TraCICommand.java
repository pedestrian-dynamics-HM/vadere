package org.vadere.manager;

import org.vadere.manager.stsc.TraCICommandBuffer;

public class TraCICommand {

	private int id;
	private TraCICommandBuffer data;

	public TraCICommand(TraCICommandBuffer buff) {
		this.id = buff.readCmdIdentifier();
		this.data = buff;
	}


	public int getId() {
		return id;
	}

}
