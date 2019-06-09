package org.vadere.manager.stsc.commands.control;

import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;

import java.nio.charset.StandardCharsets;

public class TraCISendFileCommand extends TraCICommand {

	private String file;

	public static TraCIPacket TraCISendFileCommand(String file){
		int strLen = file.getBytes(StandardCharsets.US_ASCII).length;
		TraCIPacket packet = TraCIPacket.create(); // 4 (add later)
		packet.getWriter()
				.writeCommandLength(1 + 1 + 4 + strLen) // [1|5]
				.writeUnsignedByte(TraCICmd.SEND_FILE.id) // 1
				.writeString(file); // 4+strLen
		return packet;
	}

	public TraCISendFileCommand(TraCICommandBuffer cmdBuffer) {
		super(TraCICmd.SEND_FILE);
		this.file = cmdBuffer.readString();
	}


	protected TraCISendFileCommand(TraCICmd traCICmd) {
		super(traCICmd);
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		if (NOK_response != null)
			return NOK_response;
		else
			return TraCIPacket.create(11).add_OK_StatusResponse(TraCICmd.SEND_FILE);
	}
}
