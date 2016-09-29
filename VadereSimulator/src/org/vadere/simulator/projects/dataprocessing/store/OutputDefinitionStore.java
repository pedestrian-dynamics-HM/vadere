package org.vadere.simulator.projects.dataprocessing.store;

import java.util.ArrayList;
import java.util.List;

public class OutputDefinitionStore {
    private List<OutputFileStore> files;
    private List<DataProcessorStore> processors;
    private boolean isTimestamped;

    public OutputDefinitionStore() {
        this.files = new ArrayList<>();
        this.processors = new ArrayList<>();
        this.isTimestamped = true;
    }

    public void addOutputFile(OutputFileStore file) {
        this.files.add(file);
    }

    public void addProcessor(DataProcessorStore processor) {
        this.processors.add(processor);
    }

    public void setTimestamped(boolean isTimestamped) {
        this.isTimestamped = isTimestamped;
    }
}
