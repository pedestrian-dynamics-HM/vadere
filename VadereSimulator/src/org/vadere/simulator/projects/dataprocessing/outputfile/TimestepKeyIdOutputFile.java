package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepFaceIdKey;

@OutputFileClass(dataKeyMapping = TimestepFaceIdKey.class)
public class TimestepKeyIdOutputFile extends OutputFile<TimestepFaceIdKey> {

	public TimestepKeyIdOutputFile() {
		super(TimestepFaceIdKey.getHeaders());
	}

	@Override
	public String[] toStrings(final TimestepFaceIdKey key) {
		return new String[] { Integer.toString(key.getTimeStep()), Integer.toString(key.getFaceId()) };
	}

}
