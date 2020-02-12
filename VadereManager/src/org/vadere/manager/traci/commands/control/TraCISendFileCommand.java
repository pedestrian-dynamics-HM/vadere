package org.vadere.manager.traci.commands.control;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.reader.TraCICommandBuffer;
import org.vadere.manager.traci.writer.TraCIPacket;

import java.nio.charset.StandardCharsets;

public class TraCISendFileCommand extends TraCICommand {

	private String fileName;
	private String file; // file content

	public TraCISendFileCommand(TraCICommandBuffer cmdBuffer) {
		super(TraCICmd.SEND_FILE);
		this.fileName = cmdBuffer.readString();
		this.file = cmdBuffer.readString();

	}

	protected TraCISendFileCommand(TraCICmd traCICmd) {
		super(traCICmd);
	}

	public static TraCIPacket TraCISendFileCommand(String fileName, String file) {
		int strLen = file.getBytes(StandardCharsets.US_ASCII).length;
		strLen += fileName.getBytes(StandardCharsets.US_ASCII).length;
		TraCIPacket packet = TraCIPacket.create(); // 4 (add later)
		packet.writeCommandLength(1 + 1 + 4 + 4 + strLen) // [1|5]
				.writeUnsignedByte(TraCICmd.SEND_FILE.id) // 1
				.writeString(fileName)
				.writeString(file); // 4+strLen
		return packet;
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
