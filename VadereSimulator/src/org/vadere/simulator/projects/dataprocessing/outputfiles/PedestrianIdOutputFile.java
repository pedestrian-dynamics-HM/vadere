package org.vadere.simulator.projects.dataprocessing.outputfiles;

import org.vadere.simulator.projects.dataprocessing.datakeys.PedestrianIdDataKey;

public class PedestrianIdOutputFile extends OutputFile<PedestrianIdDataKey> {

	public PedestrianIdOutputFile() {
		super(PedestrianIdDataKey.getHeader());
	}
}
