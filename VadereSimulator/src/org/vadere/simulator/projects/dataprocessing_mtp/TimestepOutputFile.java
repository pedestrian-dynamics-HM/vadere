package org.vadere.simulator.projects.dataprocessing_mtp;

public class TimestepOutputFile extends OutputFile<TimestepDataKey> {

    public TimestepOutputFile() {
        this.setKeyHeader(TimestepDataKey.getHeader());
    }

    @Override
    public String toString(TimestepDataKey key) {
        return super.toString(key);
    }
}
