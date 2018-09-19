package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOutputFile;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.mockito.Mockito.when;

public class PedestrianPositionProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdKey, VPoint> {

	PedestrianListBuilder b = new PedestrianListBuilder();

	PedestrianPositionProcessorTestEnv() {
		this(1);
	}

	PedestrianPositionProcessorTestEnv(int processorId) {
		super();
		try {
			testedProcessor = processorFactory.createDataProcessor(PedestrianPositionProcessor.class);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		testedProcessor.setId(processorId);
		this.nextProcessorId = processorId + 1;

		try {
			outputFile = outputFileFactory.createOutputfile(
					TimestepPedestrianIdOutputFile.class,
					testedProcessor.getId());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());
	}

	private void addToExpectedOutput(Integer step, List<Pedestrian> m) {
		m.forEach(p -> {
			addToExpectedOutput(new TimestepPedestrianIdKey(step, p.getId()), p.getPosition());
		});
	}

	@Override
	public void loadDefaultSimulationStateMocks() {

		addSimState(new SimulationStateMock() {
			@Override
			public void mockIt() {
				when(state.getStep()).thenReturn(1);

				b.clear().add(1, new VPoint(1.435346, 1.0))
						.add(2, new VPoint(2.0, 2.0))
						.add(3, new VPoint(3.0, 3.0));
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				addToExpectedOutput(state.getStep(), b.getList());

			}
		});

		addSimState(new SimulationStateMock() {
			@Override
			public void mockIt() {
				when(state.getStep()).thenReturn(2);
				b.clear().add(4, new VPoint(1.0, 1.0))
						.add(2, new VPoint(5.0, 5.0))
						.add(3, new VPoint(6.0, 6.0));
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				addToExpectedOutput(state.getStep(), b.getList());
			}
		});

		addSimState(new SimulationStateMock() {
			@Override
			public void mockIt() {
				when(state.getStep()).thenReturn(3);
				b.clear().add(4, new VPoint(5.0, 5.0));
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				addToExpectedOutput(state.getStep(), b.getList());
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
