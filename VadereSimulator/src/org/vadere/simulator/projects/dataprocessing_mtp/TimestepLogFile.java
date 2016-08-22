package org.vadere.simulator.projects.dataprocessing_mtp;

public class TimestepLogFile extends LogFile<TimestepDataKey> {

    public TimestepLogFile() {
        this.setKeyHeader(TimestepDataKey.getHeader());
    }
}
