package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPositionKey;

@OutputFileClass(dataKeyMapping = TimestepPositionKey.class)
public class TimestepPositionOutputFile extends OutputFile<TimestepPositionKey> {
    public TimestepPositionOutputFile() {
        super(TimestepPositionKey.getHeaders());
    }

    @Override
    public String[] toStrings(TimestepPositionKey key) {
        return new String[] { Integer.toString(key.getTimeStep()), Double.toString(key.getPosition().x), Double.toString(key.getPosition().y) };
    }
}
