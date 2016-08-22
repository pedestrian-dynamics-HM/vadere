package org.vadere.simulator.projects.dataprocessing_mtp;

public class TimestepPedestrianIdLogFile extends LogFile<TimestepPedestrianIdDataKey> {

	public TimestepPedestrianIdLogFile() {
		this.setKeyHeader(TimestepPedestrianIdDataKey.getHeader());
	}

	@Override
	public String toString(TimestepPedestrianIdDataKey key) {
		return key.getTimestep() + " " + key.getPedestrianId();
	}
}
