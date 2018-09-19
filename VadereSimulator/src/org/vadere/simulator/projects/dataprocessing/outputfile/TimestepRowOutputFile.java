package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepRowKey;

@OutputFileClass(dataKeyMapping = TimestepRowKey.class)
public class TimestepRowOutputFile extends OutputFile<TimestepRowKey> {
    public TimestepRowOutputFile() {
        super("timeStep", "row");
    }

    @Override
    public String[] toStrings(TimestepRowKey key) {
        return new String[] { Integer.toString(key.getTimeStep()), Integer.toString(key.getRow()) };
    }
}
