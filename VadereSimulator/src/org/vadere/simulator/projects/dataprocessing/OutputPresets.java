package org.vadere.simulator.projects.dataprocessing;

import java.util.Arrays;

public final class OutputPresets {
    private static OutputPresets instance;
    private final OutputDefinitionStore outputDefinition;

    private OutputPresets() {
        this.outputDefinition = new OutputDefinitionStore();

        ProcessorStore processor1 = new ProcessorStore();
        processor1.setType("org.vadere.simulator.projects.dataprocessing.PedestrianPositionProcessor");
        processor1.setId(1);
        this.outputDefinition.addProcessor(processor1);

        ProcessorStore processor2 = new ProcessorStore();
        processor2.setType("org.vadere.simulator.projects.dataprocessing.PedestrianTargetIdProcessor");
        processor2.setId(2);
        this.outputDefinition.addProcessor(processor2);

        OutputFileStore outputFile = new OutputFileStore();
        outputFile.setType("org.vadere.simulator.projects.dataprocessing.TimestepPedestrianIdOutputFile");
        outputFile.setFilename(DataProcessingJsonManager.TRAJECTORIES_FILENAME);
        outputFile.setProcessors(Arrays.asList(1, 2));
        this.outputDefinition.addOutputFile(outputFile);
    }

    public static OutputDefinitionStore getOutputDefinition() {
        return getInstance().outputDefinition;
    }

    public static OutputPresets getInstance() {
        if (instance == null) {
            instance = new OutputPresets();
        }

        return instance;
    }
}
