package org.vadere.simulator.projects.dataprocessing;

import org.vadere.simulator.projects.dataprocessing.store.DataProcessorStore;
import org.vadere.simulator.projects.dataprocessing.store.OutputDefinitionStore;
import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;
import org.vadere.state.attributes.processor.AttributesNumberOverlapsProcessor;

import java.util.Arrays;

/**
 * @author Mario Teixeira Parente
 *
 */

public final class OutputPresets {

//	private final DataProcessorFactory dataProcessorFactory;
//	private final OutputFileFactory outputFileFactory;

	public OutputPresets(){
//		dataProcessorFactory = DataProcessorFactory.instance();
//		outputFileFactory = OutputFileFactory.instance();
	}

    public OutputDefinitionStore buildDefault() {
		OutputDefinitionStore defaultPreset = new OutputDefinitionStore();

        DataProcessorStore processor1 = new DataProcessorStore();
        processor1.setType("org.vadere.simulator.projects.dataprocessing.processor.FootStepProcessor");
        processor1.setId(1);
		defaultPreset.addProcessor(processor1);

        DataProcessorStore processor2 = new DataProcessorStore();
        processor2.setType("org.vadere.simulator.projects.dataprocessing.processor.FootStepTargetIDProcessor");
        processor2.setId(2);
		defaultPreset.addProcessor(processor2);

        OutputFileStore outputFile = new OutputFileStore();
        outputFile.setType("org.vadere.simulator.projects.dataprocessing.outputfile.EventtimePedestrianIdOutputFile");
        outputFile.setFilename(DataProcessingJsonManager.TRAJECTORIES_FILENAME);
        outputFile.setProcessors(Arrays.asList(1, 2));
		defaultPreset.addOutputFile(outputFile);

		defaultPreset.setTimestamped(true);

		return defaultPreset;
    }

    public OutputDefinitionStore withOverlapProcessor(){
		OutputDefinitionStore store = buildDefault();
		DataProcessorStore processor3 = new DataProcessorStore();
		processor3.setType("org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapProcessor");
		processor3.setId(3);
		store.addProcessor(processor3);

		DataProcessorStore processor4 = new DataProcessorStore();
		AttributesNumberOverlapsProcessor attr4 = new AttributesNumberOverlapsProcessor();
		attr4.setPedestrianOverlapProcessorId(3);
		processor4.setType("org.vadere.simulator.projects.dataprocessing.processor.NumberOverlapsProcessor");
		processor4.setId(4);
		processor4.setAttributesType("org.vadere.state.attributes.processor.AttributesNumberOverlapsProcessor");
		processor4.setAttributes(attr4);
		store.addProcessor(processor4);

		OutputFileStore outputFilePedOverlap = new OutputFileStore();
		outputFilePedOverlap.setType("org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOverlapOutputFile");
		outputFilePedOverlap.setFilename("overlaps.csv");
		outputFilePedOverlap.setProcessors(Arrays.asList(3));
		store.addOutputFile(outputFilePedOverlap);

		OutputFileStore outputFileOverlapCount = new OutputFileStore();
		outputFileOverlapCount.setType("org.vadere.simulator.projects.dataprocessing.outputfile.NoDataKeyOutputFile");
		outputFileOverlapCount.setFilename("overlapCount.txt");
		outputFileOverlapCount.setProcessors(Arrays.asList(4));
		store.addOutputFile(outputFileOverlapCount);

		return store;
	}

	// create default Scenario File with overlap processors added automatically.
    public static OutputDefinitionStore getOutputDefinition() {
        return new OutputPresets().withOverlapProcessor();
    }

}
