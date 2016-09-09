package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdDataKey;

public class PedestrianIdOutputFile extends OutputFile<PedestrianIdDataKey> {

	public PedestrianIdOutputFile() {
		super(PedestrianIdDataKey.getHeader());
	}
}
