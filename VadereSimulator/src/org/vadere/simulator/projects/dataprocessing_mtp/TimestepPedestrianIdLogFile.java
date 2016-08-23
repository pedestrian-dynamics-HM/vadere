package org.vadere.simulator.projects.dataprocessing_mtp;

public class TimestepPedestrianIdLogFile extends LogFile<TimestepPedestrianIdDataKey> {

	public TimestepPedestrianIdLogFile() {
		this.setKeyHeader(TimestepPedestrianIdDataKey.getHeader());
	}

	@Override
	public String toString(final TimestepPedestrianIdDataKey key) {
		return key.getTimestep() + LogFile.SEPARATOR.toString() + key.getPedestrianId();
	}
}
