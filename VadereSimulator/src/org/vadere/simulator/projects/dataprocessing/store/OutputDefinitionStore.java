package org.vadere.simulator.projects.dataprocessing.store;

import java.util.ArrayList;
import java.util.List;

public class OutputDefinitionStore {
    private List<OutputFileStore> files;
    private List<DataProcessorStore> processors;

    public OutputDefinitionStore() {
        this.files = new ArrayList<>();
        this.processors = new ArrayList<>();
    }

    public void addOutputFile(OutputFileStore file) {
        this.files.add(file);
    }

    public void addProcessor(DataProcessorStore processor) {
        this.processors.add(processor);
    }
}
