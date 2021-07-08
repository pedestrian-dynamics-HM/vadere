package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepIdDataKey;

@OutputFileClass(dataKeyMapping = TimestepIdDataKey.class)
public class TimestepIdDataOutputFile extends OutputFile<TimestepIdDataKey> {

    public TimestepIdDataOutputFile() {
        super(TimestepIdDataKey.getHeaders());
    }

    @Override
    public String[] toStrings(final TimestepIdDataKey key) {
        return new String[] { Integer.toString(key.getTimestep()), Integer.toString(key.getId()) };
    }
}
