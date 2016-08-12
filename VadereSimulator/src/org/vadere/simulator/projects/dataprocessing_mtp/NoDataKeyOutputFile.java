package org.vadere.simulator.projects.dataprocessing_mtp;

public class NoDataKeyOutputFile extends OutputFile<NoDataKey> {
    public NoDataKeyOutputFile() {
        this.setKeyHeader(NoDataKey.getHeader());
    }

    @Override
    public String toString(NoDataKey key) {
        return "";
    }
}
