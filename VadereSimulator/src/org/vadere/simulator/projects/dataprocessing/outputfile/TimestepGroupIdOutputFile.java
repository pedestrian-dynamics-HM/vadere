package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepGroupIdKey;

/**
 * @author Manuel Hertle
 *
 */
@OutputFileClass(dataKeyMapping = TimestepGroupIdKey.class)
public class TimestepGroupIdOutputFile extends OutputFile<TimestepGroupIdKey> {

    public TimestepGroupIdOutputFile() {
        super(TimestepGroupIdKey.getHeaders());
    }

    @Override
    public String[] toStrings(final TimestepGroupIdKey key) {
        return new String[] { Integer.toString(key.getTimestep()), Integer.toString(key.getGroupId()) };
    }
}
