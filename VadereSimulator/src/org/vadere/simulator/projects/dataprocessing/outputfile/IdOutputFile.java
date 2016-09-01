package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.datakey.IdDataKey;

public class IdOutputFile extends OutputFile<IdDataKey> {

	public IdOutputFile() {
		super(IdDataKey.getHeaders());
	}

	@Override
	public String[] toStrings(final IdDataKey key) {
		return new String[] { Integer.toString(key.getId()) };
	}
}
