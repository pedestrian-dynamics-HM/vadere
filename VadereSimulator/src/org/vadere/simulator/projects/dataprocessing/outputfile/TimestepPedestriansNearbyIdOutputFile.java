package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdOverlapKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestriansNearbyIdKey;

@OutputFileClass(dataKeyMapping = TimestepPedestriansNearbyIdKey.class)
public class TimestepPedestriansNearbyIdOutputFile extends OutputFile<TimestepPedestriansNearbyIdKey>{

	public TimestepPedestriansNearbyIdOutputFile() {
		super(TimestepPedestriansNearbyIdKey.getHeaders());
	}

	@Override
	public String[] toStrings(final TimestepPedestriansNearbyIdKey key){
		return key.toStrings();
	}
}
