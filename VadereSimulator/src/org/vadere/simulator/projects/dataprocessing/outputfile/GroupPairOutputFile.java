package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepGroupPairKey;

@OutputFileClass(dataKeyMapping = TimestepGroupPairKey.class )
public class GroupPairOutputFile extends OutputFile<TimestepGroupPairKey>{
	public GroupPairOutputFile(String... keyHeaders) {
		super(TimestepGroupPairKey.getHeaders());
	}

	@Override
	public String[] toStrings(TimestepGroupPairKey key) {
		return key.toStrings();
	}
}

