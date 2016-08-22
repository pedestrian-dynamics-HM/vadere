package org.vadere.simulator.projects.dataprocessing_mtp;

public class PedestrianIdLogFile extends LogFile<PedestrianIdDataKey> {

	public PedestrianIdLogFile() {
		this.setKeyHeader(PedestrianIdDataKey.getHeader());
	}

}
