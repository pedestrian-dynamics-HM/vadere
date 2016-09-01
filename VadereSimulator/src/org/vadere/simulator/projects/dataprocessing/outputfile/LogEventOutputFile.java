package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.datakey.IdDataKey;

public class LogEventOutputFile extends OutputFile<IdDataKey> {

	public LogEventOutputFile() {
		super("ID");
	}

	@Override
	public String[] toStrings(final IdDataKey key) {
		return new String[] { Integer.toString(key.getId()) };
	}
}
