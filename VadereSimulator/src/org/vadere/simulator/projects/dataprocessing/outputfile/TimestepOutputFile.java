package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;

/**
 * @author Mario Teixeira Parente
 *
 */
@OutputFileClass(dataKeyMapping = TimestepKey.class)
public class TimestepOutputFile extends OutputFile<TimestepKey> {

    public TimestepOutputFile() {
        super("timeStep");
    }
}
