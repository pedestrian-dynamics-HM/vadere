package org.vadere.manager.traci.commands.control;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.reader.TraCICommandBuffer;
import org.vadere.util.logging.Logger;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

public class TraCISendFileCommandV20_0_1 extends TraCISendFileCommand {
	private static Logger logger = Logger.getLogger(TraCISendFileCommandV20_0_1.class);

	HashMap<String, ByteArrayInputStream> cacheData;


	public TraCISendFileCommandV20_0_1(TraCICommandBuffer cmdBuffer) {
		super(cmdBuffer);
		cacheData = new HashMap<>();
		if (cmdBuffer.hasRemaining()) {
			int numberOfCaches = cmdBuffer.readInt();
			for (int i = 0; i < numberOfCaches; i++) {
				String cacheIdentifier = cmdBuffer.readString();
				int numberOfBytes = cmdBuffer.readInt();
				byte[] data = new byte[numberOfBytes];
				cmdBuffer.readBytes(data);
				cacheData.put(cacheIdentifier, new ByteArrayInputStream(data));
			}
		} else {
			logger.warnf("did not found fields needed for cache extraction fallback to old version and treat as zero cache send.");
		}

	}

	protected TraCISendFileCommandV20_0_1(TraCICmd traCICmd) {
		super(traCICmd);
		cacheData = new HashMap<>();
	}

	public HashMap<String, ByteArrayInputStream> getCacheData() {
		return cacheData;
	}
}
