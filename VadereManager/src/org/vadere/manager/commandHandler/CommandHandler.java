package org.vadere.manager.commandHandler;

import org.vadere.manager.TraCiMessageBuilder;
import org.vadere.manager.TraciCommand;

public interface CommandHandler {

	boolean handelCommand(TraciCommand cmd, TraCiMessageBuilder msgBuilder);

	default void writeStatusCmd(TraCiMessageBuilder msgBuilder, final int commandId, final int status, final String description){
		// cmd length (Byte) + commandId (Byte) + status (Byte) + String LenField (Integer) + dataLen (i.e. description)
//		msgBuilder.writeUnsignedByte( 1 + 1 + 1 + 4 + description.length());
		msgBuilder.writeUnsignedByte(0x11);
		msgBuilder.writeUnsignedByte(0x22);
		msgBuilder.writeStringASCII(description);
	}
}
