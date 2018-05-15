package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.IdDataKey;

@OutputFileClass(dataKeyMapping = IdDataKey.class)
public class IdOutputFile extends OutputFile<IdDataKey> {

	public IdOutputFile() {
		super(IdDataKey.getHeaders());
	}

	@Override
	public String[] toStrings(final IdDataKey key) {
		return new String[] { Integer.toString(key.getId()) };
	}
}
