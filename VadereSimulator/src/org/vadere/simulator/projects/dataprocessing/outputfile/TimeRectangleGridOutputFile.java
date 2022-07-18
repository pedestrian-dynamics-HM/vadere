package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimeRectangleGridKey;

@OutputFileClass(dataKeyMapping = TimeRectangleGridKey.class)
public class TimeRectangleGridOutputFile extends OutputFile<TimeRectangleGridKey> {

    public TimeRectangleGridOutputFile(String... dataIndices) {
        super(TimeRectangleGridKey.getHeaders());
    }

    public String[] toStrings(TimeRectangleGridKey key) {
        return key.toStrings();
    }
}
