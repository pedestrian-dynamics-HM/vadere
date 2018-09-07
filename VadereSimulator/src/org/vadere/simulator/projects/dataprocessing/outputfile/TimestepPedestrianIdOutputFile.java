package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;

/**
 * @author Mario Teixeira Parente
 *
 */
@OutputFileClass(dataKeyMapping = TimestepPedestrianIdKey.class)
public class TimestepPedestrianIdOutputFile extends OutputFile<TimestepPedestrianIdKey> {

	public TimestepPedestrianIdOutputFile() {
		super(TimestepPedestrianIdKey.getHeaders());
	}

	@Override
	public String[] toStrings(final TimestepPedestrianIdKey key) {
		return new String[] { Integer.toString(key.getTimestep()), Integer.toString(key.getPedestrianId()) };
	}
}
