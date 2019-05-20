package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
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

	private EvacuationTimeProcessorTestEnv(int nextProcessorId) {
		super(EvacuationTimeProcessor.class, NoDataKey.class, nextProcessorId);
	}

	@Override
	void initializeDependencies() {
		AttributesEvacuationTimeProcessor attr = (AttributesEvacuationTimeProcessor) testedProcessor.getAttributes();
		int pedEvacTimeProcId = addDependentProcessor(PedestrianEvacuationTimeProcessorTestEnv::new);
		attr.setPedestrianEvacuationTimeProcessorId(pedEvacTimeProcId);
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

				addToExpectedOutput(NoDataKey.key(), Double.POSITIVE_INFINITY);

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
