package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;

/**
 *
 */
@OutputFileClass(dataKeyMapping = EventtimePedestrianIdKey.class)
public class EventtimePedestrianIdOutputFile extends OutputFile<EventtimePedestrianIdKey> {

	public EventtimePedestrianIdOutputFile() {
		super(EventtimePedestrianIdKey.getHeaders());
	}

	@Override
	public String[] toStrings(final EventtimePedestrianIdKey key) {
		return new String[] {Integer.toString(key.getPedestrianId()), Double.toString(key.getSimtime())};
	}
}
