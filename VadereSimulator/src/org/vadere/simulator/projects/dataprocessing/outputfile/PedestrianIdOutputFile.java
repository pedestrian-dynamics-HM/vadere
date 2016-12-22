package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;

/**
 * @author Mario Teixeira Parente
 *
 */

public class PedestrianIdOutputFile extends OutputFile<PedestrianIdKey> {

	public PedestrianIdOutputFile() {
		super(PedestrianIdKey.getHeader());
	}
}
