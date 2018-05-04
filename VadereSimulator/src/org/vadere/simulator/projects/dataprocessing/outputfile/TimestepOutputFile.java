package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;

/**
 * @author Mario Teixeira Parente
 *
 */
@OutputFileClass()
public class TimestepOutputFile extends OutputFile<TimestepKey> {

    public TimestepOutputFile() {
        super("hdfkh");
    }
}
