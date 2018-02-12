package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.VadereStringWriter;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOutputFile;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.mockito.Mockito.when;

public class PedestrianPositionProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdKey, VPoint> {

	PedestrianPositionProcessorTestEnv() {
		super();

		testedProcessor = processorFactory.createDataProcessor(PedestrianPositionProcessor.class);
		testedProcessor.setId(nextProcessorId++);

		outputFile = outputFileFactory.createOutputfile(
				TimestepPedestrianIdOutputFile.class,
				testedProcessor.getId());
		outputFile.setVadereWriter(new VadereStringWriter());
	}


	private void addToExpectedOutput(Integer step, Map<Integer, VPoint> m) {
		m.entrySet().stream().distinct().sorted(Map.Entry.comparingByKey()).forEach((v) -> {
			super.addToExpectedOutput(
					new TimestepPedestrianIdKey(step, v.getKey()),
					v.getValue());
		});

	}

	@Override
	public void loadDefaultSimulationStateMocks() {

		states.add(new SimulationStateMock() {
			@Override
			public void mockIt() {
				when(state.getStep()).thenReturn(1);
				Map<Integer, VPoint> m = new HashMap<>();
				m.put(1, new VPoint(1.435346, 1.0));
				m.put(2, new VPoint(2.0, 2.0));
				m.put(3, new VPoint(3.0, 3.0));
				addToExpectedOutput(state.getStep(), m);
				when(state.getPedestrianPositionMap()).thenReturn(m);
			}
		});

		states.add(new SimulationStateMock() {
			@Override
			public void mockIt() {
				when(state.getStep()).thenReturn(2);
				Map<Integer, VPoint> m = new HashMap<>();
				m.put(4, new VPoint(1.0, 1.0));
				m.put(2, new VPoint(5.0, 5.0));
				m.put(3, new VPoint(6.0, 6.0));
				addToExpectedOutput(state.getStep(), m);
				when(state.getPedestrianPositionMap()).thenReturn(m);
			}
		});

		states.add(new SimulationStateMock() {
			@Override
			public void mockIt() {
				when(state.getStep()).thenReturn(3);
				Map<Integer, VPoint> m = new HashMap<>();
				m.put(4, new VPoint(5.0, 5.0));
				addToExpectedOutput(state.getStep(), m);
				when(state.getPedestrianPositionMap()).thenReturn(m);
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
					sj.add(Integer.toString(e.getKey().getTimestep()))
							.add(Integer.toString(e.getKey().getPedestrianId()))
							.add(Double.toString(e.getValue().x))
							.add(Double.toString(e.getValue().y));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
