package org.vadere.manager.traci.sumo;

import org.vadere.state.traci.TraCIException;

public enum LightPhase {

	RED(0x01),
	YELLOW(0x02),
	GREEN(0x03),
	OFF_BLINK(0x04),
	OFF(0x05);


	public int id;

	LightPhase(int id) {
		this.id = id;
	}

	public static LightPhase fromId(int id) {
		for (LightPhase value : values()) {
			if (value.id == id)
				return value;
		}
		throw new TraCIException("No LightPhase for traCICmd: " + id);
	}

}
