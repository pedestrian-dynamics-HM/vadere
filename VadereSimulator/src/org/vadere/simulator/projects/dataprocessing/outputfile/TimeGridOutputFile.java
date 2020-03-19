package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimeGridKey;

@OutputFileClass(dataKeyMapping = TimeGridKey.class)
public class TimeGridOutputFile extends OutputFile<TimeGridKey> {

    public TimeGridOutputFile(String... dataIndices) {
        super(TimeGridKey.getHeaders());
    }

    public String[] toStrings(TimeGridKey key) {
        return key.toStrings();
    }
}
