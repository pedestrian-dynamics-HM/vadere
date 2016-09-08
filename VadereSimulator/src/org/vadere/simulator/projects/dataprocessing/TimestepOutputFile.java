package org.vadere.simulator.projects.dataprocessing;

public class TimestepOutputFile extends OutputFile<TimestepDataKey> {

    public TimestepOutputFile() {
        super(TimestepDataKey.getHeader());
    }
}
