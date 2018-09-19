package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.state.attributes.processor.AttributesMeanPedestrianEvacuationTimeProcessor;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class MeanPedestrianEvacuationTimeProcessorTestEnv extends ProcessorTestEnv<NoDataKey, Double> {

	private PedestrianListBuilder b = new PedestrianListBuilder();

	@SuppressWarnings("unchecked")
	MeanPedestrianEvacuationTimeProcessorTestEnv() {
		try {
			testedProcessor = processorFactory.createDataProcessor(MeanPedestrianEvacuationTimeProcessor.class);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		testedProcessor.setId(nextProcessorId());

		DataProcessor pedEvacTimeProc;
		PedestrianEvacuationTimeProcessorTestEnv pedEvacTimeProcEnv;
		int pedEvacTimeProcId = nextProcessorId();

		//add ProcessorId of required Processors to current Processor under test
		AttributesMeanPedestrianEvacuationTimeProcessor attr = (AttributesMeanPedestrianEvacuationTimeProcessor) testedProcessor.getAttributes();
		attr.setPedestrianEvacuationTimeProcessorId(pedEvacTimeProcId);

		//create required Processor enviroment and add it to current Processor under test
		pedEvacTimeProcEnv = new PedestrianEvacuationTimeProcessorTestEnv(pedEvacTimeProcId);
		pedEvacTimeProc = pedEvacTimeProcEnv.getTestedProcessor();

		Mockito.when(manager.getProcessor(pedEvacTimeProcId)).thenReturn(pedEvacTimeProc);
		addRequiredProcessors(pedEvacTimeProcEnv);

		//setup output file with different VadereWriter impl for test
		try {
			outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
					NoDataKey.class,
					testedProcessor.getId()
			);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());

	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				b.clear().add(1, 2, 3);
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getSimTimeInSec()).thenReturn(0.0);
			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {
				b.clear().add(1, 2, 3, 4, 5);
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getSimTimeInSec()).thenReturn(5.0);
			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {
				b.clear().add(1, 2, 3, 4, 5);
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getSimTimeInSec()).thenReturn(10.0);
			}
		});

		addSimState(new SimulationStateMock(4) {
			@Override
			public void mockIt() {
				b.clear();
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getSimTimeInSec()).thenReturn(15.0);

				double meanEvacTime = (3 * (10.0 - 0.0) + 2 * (10.0 - 5.0)) / 5;

				addToExpectedOutput(NoDataKey.key(), meanEvacTime);
			}
		});
	}

	@Override
	List<String> getExpectedOutputAsList() {
		List<String> outputList = new ArrayList<>();
		expectedOutput.entrySet()
				.stream()
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.forEach(e -> {
					StringJoiner sj = new StringJoiner(getDelimiter());
					sj.add(Double.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
