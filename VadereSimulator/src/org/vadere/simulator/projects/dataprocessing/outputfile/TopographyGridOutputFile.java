package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.TopographyGridKey;

@OutputFileClass(dataKeyMapping = TopographyGridKey.class)
public class TopographyGridOutputFile extends OutputFile<TopographyGridKey> {

    public TopographyGridOutputFile() {
        super(TopographyGridKey.getHeaders());
    }

    @Override
    public String[] toStrings(final TopographyGridKey key) {
        return new String[] { Integer.toString(key.getXId()), Integer.toString(key.getYId()) };
    }
}
