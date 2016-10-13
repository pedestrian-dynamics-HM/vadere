package org.vadere.simulator.models.seating.dataprocessing;

import org.vadere.simulator.projects.dataprocessing.datakey.IdDataKey;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;

public class LogEventOutputFile extends OutputFile<IdDataKey> {

	public LogEventOutputFile() {
		super("ID");
	}

	@Override
	public String[] toStrings(final IdDataKey key) {
		return new String[] { Integer.toString(key.getId()) };
	}
}
