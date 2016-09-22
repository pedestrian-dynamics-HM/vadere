package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdDataKey;

/**
 * @author Mario Teixeira Parente
 *
 */

public class PedestrianIdOutputFile extends OutputFile<PedestrianIdDataKey> {

	public PedestrianIdOutputFile() {
		super(PedestrianIdDataKey.getHeader());
	}
}
