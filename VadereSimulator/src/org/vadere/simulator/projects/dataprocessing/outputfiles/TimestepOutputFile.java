package org.vadere.simulator.projects.dataprocessing.outputfiles;

import org.vadere.simulator.projects.dataprocessing.datakeys.TimestepDataKey;

public class TimestepOutputFile extends OutputFile<TimestepDataKey> {

    public TimestepOutputFile() {
        super(TimestepDataKey.getHeader());
    }
}
