package org.vadere.simulator.projects.dataprocessing.outputfiles;

import org.vadere.simulator.projects.dataprocessing.datakeys.NoDataKey;

public class NoDataKeyOutputFile extends OutputFile<NoDataKey> {
    public NoDataKeyOutputFile() {
        super(new String[] { });
    }

    @Override
    public String[] toStrings(NoDataKey key) {
        return new String[] { };
    }
}
