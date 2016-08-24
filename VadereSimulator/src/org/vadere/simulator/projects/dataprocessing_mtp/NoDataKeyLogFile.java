package org.vadere.simulator.projects.dataprocessing_mtp;

public class NoDataKeyLogFile extends LogFile<NoDataKey> {
    public NoDataKeyLogFile() {
        super(new String[] { });
    }

    @Override
    public String[] toStrings(NoDataKey key) {
        return new String[] { };
    }
}
