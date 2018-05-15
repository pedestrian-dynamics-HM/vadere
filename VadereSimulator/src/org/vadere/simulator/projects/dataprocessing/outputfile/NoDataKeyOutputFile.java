package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;

/**
 * @author Mario Teixeira Parente
 *
 */
@OutputFileClass(dataKeyMapping = NoDataKey.class)
public class NoDataKeyOutputFile extends OutputFile<NoDataKey> {
    public NoDataKeyOutputFile() {
        super();
    }

    @Override
    public String[] toStrings(NoDataKey key) {
        return new String[] { };
    }
}
