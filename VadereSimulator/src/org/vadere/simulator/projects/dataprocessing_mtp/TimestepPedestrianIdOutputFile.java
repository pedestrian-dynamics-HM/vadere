package org.vadere.simulator.projects.dataprocessing_mtp;

public class TimestepPedestrianIdOutputFile extends OutputFile<TimestepPedestrianIdDataKey> {

	public TimestepPedestrianIdOutputFile() {
		this.setKeyHeader(TimestepPedestrianIdDataKey.getHeader());
	}

	@Override
	public String toString(TimestepPedestrianIdDataKey key) {
		return key.getTimestep() + " " + key.getPedestrianId();
	}
}
