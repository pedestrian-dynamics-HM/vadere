package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;

public class NoDataKeyOutputFile extends OutputFile<NoDataKey> {
    public NoDataKeyOutputFile() {
        super(new String[] { });
    }

    @Override
    public String[] toStrings(NoDataKey key) {
        return new String[] { };
    }
}
