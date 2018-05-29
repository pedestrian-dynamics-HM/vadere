package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.state.attributes.processor.AttributesEvacuationTimeProcessor;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.mockito.Mockito.when;

public class EvacuationTimeProcessorTestEnv extends ProcessorTestEnv<NoDataKey, Double> {

	private PedestrianListBuilder b = new PedestrianListBuilder();

	EvacuationTimeProcessorTestEnv() {
		this(1);
	}

	@SuppressWarnings("unchecked")
	private EvacuationTimeProcessorTestEnv(int nextProcessorId) {
		testedProcessor = processorFactory.createDataProcessor(EvacuationTimeProcessor.class);
		testedProcessor.setId(nextProcessorId);
		this.nextProcessorId = nextProcessorId + 1;

		DataProcessor pedEvacTimeProc;
		PedestrianEvacuationTimeProcessorTestEnv pedEvacTimeProcEnv;
		int pedEvacTimeProcId = nextProcessorId();

		//add ProcessorId of required Processors to current Processor under test
		AttributesEvacuationTimeProcessor attr = (AttributesEvacuationTimeProcessor) testedProcessor.getAttributes();
		attr.setPedestrianEvacuationTimeProcessorId(pedEvacTimeProcId);

		//create required Processor enviroment and add it to current Processor under test
		pedEvacTimeProcEnv = new PedestrianEvacuationTimeProcessorTestEnv(pedEvacTimeProcId);
		pedEvacTimeProc = pedEvacTimeProcEnv.getTestedProcessor();

		Mockito.when(manager.getProcessor(pedEvacTimeProcId)).thenReturn(pedEvacTimeProc);
		addRequiredProcessors(pedEvacTimeProcEnv);

		//setup output file with different VadereWriter impl for test
		outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
				NoDataKey.class,
				testedProcessor.getId()
		);
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());

	}

	void loadSimulationStateMocksNaN() {
		clearStates();
		loadDefaultSimulationStateMocks();
		removeState(3);
		addSimState(new SimulationStateMock(4) {
			@Override
			public void mockIt() {

				b.clear().add(new Integer[]{1});

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(40.0);

				addToExpectedOutput(NoDataKey.key(), Double.NaN);

			}
		});
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {

				b.clear().add(1, 3, 4);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(0.4);

			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {

				b.clear().add(1, 3, 4, 5, 8);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(12.8);

			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {

				b.clear().add(1, 5, 8);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(34.7);


			}
		});


		addSimState(new SimulationStateMock(4) {
			@Override
			public void mockIt() {

				b.clear();

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(40.0);

				addToExpectedOutput(NoDataKey.key(), 34.7 - 0.4);

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
