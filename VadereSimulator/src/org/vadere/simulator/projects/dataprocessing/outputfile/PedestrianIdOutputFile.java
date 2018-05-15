package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;

/**
 * @author Mario Teixeira Parente
 *
 */
@OutputFileClass(dataKeyMapping = PedestrianIdKey.class)
public class PedestrianIdOutputFile extends OutputFile<PedestrianIdKey> {

	public PedestrianIdOutputFile() {
		super(PedestrianIdKey.getHeader());
	}
}
