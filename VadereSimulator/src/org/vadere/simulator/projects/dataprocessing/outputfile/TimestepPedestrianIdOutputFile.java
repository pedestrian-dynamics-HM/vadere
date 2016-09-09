package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdDataKey;

public class TimestepPedestrianIdOutputFile extends OutputFile<TimestepPedestrianIdDataKey> {

	public TimestepPedestrianIdOutputFile() {
		super(TimestepPedestrianIdDataKey.getHeaders());
	}

	@Override
	public String[] toStrings(final TimestepPedestrianIdDataKey key) {
		return new String[] { Integer.toString(key.getTimestep()), Integer.toString(key.getPedestrianId()) };
	}
}
