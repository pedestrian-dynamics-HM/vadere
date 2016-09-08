package org.vadere.simulator.projects.dataprocessing;

public class NoDataKeyOutputFile extends OutputFile<NoDataKey> {
    public NoDataKeyOutputFile() {
        super(new String[] { });
    }

    @Override
    public String[] toStrings(NoDataKey key) {
        return new String[] { };
    }
}
