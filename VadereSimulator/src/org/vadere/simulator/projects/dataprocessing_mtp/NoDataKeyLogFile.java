package org.vadere.simulator.projects.dataprocessing_mtp;

public class NoDataKeyLogFile extends LogFile<NoDataKey> {
    public NoDataKeyLogFile() {
        this.setKeyHeader(NoDataKey.getHeader());
    }

    @Override
    public String toString(NoDataKey key) {
        return "";
    }
}
