package org.vadere.manager.commandHandler;


import de.tudresden.sumo.config.Constants;
import de.uniluebeck.itm.tcpip.Storage;

public class GetVersionCmdHandler implements CommandHandler{


	@Override
	public boolean handelCommand(Storage inputStorage, Storage outputStorage) {


		writeStatusCmd(outputStorage, Constants.CMD_GETVERSION, Constants.RTYPE_OK, "");

		Storage tmp = new Storage();
		tmp.writeInt(Constants.TRACI_VERSION);
		tmp.writeStringASCII("Vaderer TraCI Server" );

		// cmdLen + cmdID + data (Int + String)
		outputStorage.writeUnsignedByte(1 + 1 + tmp.size());
		outputStorage.writeUnsignedByte(Constants.CMD_GETVERSION);
		for (Byte b : tmp.getStorageList())
			outputStorage.writeByte(b);

		return true;
	}
}
