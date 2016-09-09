package org.vadere.simulator.projects.dataprocessing.outputfiles;

import org.vadere.simulator.projects.dataprocessing.datakeys.TimestepPedestrianIdDataKey;

public class TimestepPedestrianIdOutputFile extends OutputFile<TimestepPedestrianIdDataKey> {

	public TimestepPedestrianIdOutputFile() {
		super(TimestepPedestrianIdDataKey.getHeaders());
	}

	@Override
	public String[] toStrings(final TimestepPedestrianIdDataKey key) {
		return new String[] { Integer.toString(key.getTimestep()), Integer.toString(key.getPedestrianId()) };
	}
}
