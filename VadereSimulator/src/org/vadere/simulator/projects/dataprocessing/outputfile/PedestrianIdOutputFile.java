package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;

/**
 * @author Mario Teixeira Parente
 *
 */
@OutputFileClass()
public class PedestrianIdOutputFile extends OutputFile<PedestrianIdKey> {

	public PedestrianIdOutputFile() {
		super(PedestrianIdKey.getHeader());
	}
}
