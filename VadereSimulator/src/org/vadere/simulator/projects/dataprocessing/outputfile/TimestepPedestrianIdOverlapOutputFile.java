package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdOverlap;

@OutputFileClass(dataKeyMapping = TimestepPedestrianIdOverlap.class)
public class TimestepPedestrianIdOverlapOutputFile extends OutputFile<TimestepPedestrianIdOverlap>{

	public TimestepPedestrianIdOverlapOutputFile() {
		super(TimestepPedestrianIdOverlap.getHeaders());
	}

	@Override
	public String[] toStrings(final TimestepPedestrianIdOverlap key){
		return key.toStrings();
	}
}
