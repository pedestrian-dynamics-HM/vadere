package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdOverlapKey;

@OutputFileClass(dataKeyMapping = TimestepPedestrianIdOverlapKey.class)
public class TimestepPedestrianIdOverlapOutputFile extends OutputFile<TimestepPedestrianIdOverlapKey>{

	public TimestepPedestrianIdOverlapOutputFile() {
		super(TimestepPedestrianIdOverlapKey.getHeaders());
	}

	@Override
	public String[] toStrings(final TimestepPedestrianIdOverlapKey key){
		return key.toStrings();
	}
}
