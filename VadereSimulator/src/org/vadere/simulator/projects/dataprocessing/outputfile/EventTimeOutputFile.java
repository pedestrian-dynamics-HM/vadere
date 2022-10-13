package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.EventTimeKey;

/**
 *
 */
@OutputFileClass(dataKeyMapping = EventTimeKey.class)
public class EventTimeOutputFile extends OutputFile<EventTimeKey> {

    public EventTimeOutputFile() {
        super(EventTimeKey.getHeaders());
    }

    @Override
    public String[] toStrings(final EventTimeKey key) {
        return new String[] {Double.toString(key.getSimTime())};
    }
}
