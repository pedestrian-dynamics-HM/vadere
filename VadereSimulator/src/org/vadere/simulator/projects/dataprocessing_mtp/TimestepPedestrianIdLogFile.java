package org.vadere.simulator.projects.dataprocessing_mtp;

public class TimestepPedestrianIdLogFile extends LogFile<TimestepPedestrianIdDataKey> {

	public TimestepPedestrianIdLogFile() {
		super(TimestepPedestrianIdDataKey.getHeaders());
	}

	@Override
	public String[] toStrings(final TimestepPedestrianIdDataKey key) {
		return new String[] { Integer.toString(key.getTimestep()), Integer.toString(key.getPedestrianId()) };
	}
}
