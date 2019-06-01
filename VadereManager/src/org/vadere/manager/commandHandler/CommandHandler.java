package org.vadere.manager.commandHandler;

import de.uniluebeck.itm.tcpip.Storage;

public interface CommandHandler {

	boolean handelCommand(Storage inputStorage, Storage outputStorage);

	default void writeStatusCmd(Storage storage, final int commandId, final int status, final String description){
		// cmd length (Byte) + commandId (Byte) + status (Byte) + String LenField (Integer) + dataLen (i.e. description)
		storage.writeUnsignedByte( 1 + 1 + 1 + 4 + description.length());
		storage.writeUnsignedByte(commandId);
		storage.writeUnsignedByte(status);
		storage.writeStringASCII(description);
	}
}
