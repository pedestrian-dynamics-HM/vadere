package org.vadere.simulator.projects.dataprocessing;

import org.vadere.simulator.projects.dataprocessing.store.OutputDefinitionStore;
import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;
import org.vadere.simulator.projects.dataprocessing.store.DataProcessorStore;

import java.util.Arrays;

/**
 * @author Mario Teixeira Parente
 *
 */

public final class OutputPresets {
    private static OutputPresets instance;
    private final OutputDefinitionStore outputDefinition;

    private OutputPresets() {
        this.outputDefinition = new OutputDefinitionStore();

        DataProcessorStore processor1 = new DataProcessorStore();
        processor1.setType("org.vadere.simulator.projects.dataprocessing.processor.PedestrianPositionProcessor");
        processor1.setId(1);
        this.outputDefinition.addProcessor(processor1);

        DataProcessorStore processor2 = new DataProcessorStore();
        processor2.setType("org.vadere.simulator.projects.dataprocessing.processor.PedestrianTargetIdProcessor");
        processor2.setId(2);
        this.outputDefinition.addProcessor(processor2);

        OutputFileStore outputFile = new OutputFileStore();
        outputFile.setType("org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOutputFile");
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
