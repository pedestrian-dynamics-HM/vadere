package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.processor.AttributesPedestrianLastPositionProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.mockito.Mockito.when;

public class PedestrianLastPositionProcessorTestEnv extends ProcessorTestEnv<PedestrianIdKey, VPoint> {

	PedestrianListBuilder b = new PedestrianListBuilder();

	PedestrianLastPositionProcessorTestEnv() {
		super(PedestrianLastPositionProcessor.class, PedestrianIdKey.class);
	}

	@Override
	void initializeDependencies() {
		int pedPosProcId = addDependentProcessor(PedestrianPositionProcessorTestEnv::new);
		//add ProcessorId of required Processors to current Processor under test
		((AttributesPedestrianLastPositionProcessor) testedProcessor.getAttributes())
				.setPedestrianPositionProcessorId(pedPosProcId);
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.0, 1.2), 0)
						.add(3, new VPoint(4.45, 1.2), 0)
						.add(5, new VPoint(3.546, 7.2342), 0);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());

				addToExpectedOutput(new PedestrianIdKey(1), new VPoint(1.0, 1.2));
				addToExpectedOutput(new PedestrianIdKey(3), new VPoint(4.45, 1.2));
				addToExpectedOutput(new PedestrianIdKey(5), new VPoint(3.546, 7.2342));

			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(33.2, 3.22), 0)
						.add(3, new VPoint(3.2, 22.3), 0)
						.add(7, new VPoint(1.2, 3.3), 0);
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());

				//overwrite Values!
				addToExpectedOutput(new PedestrianIdKey(1), new VPoint(33.2, 3.22));
				addToExpectedOutput(new PedestrianIdKey(3), new VPoint(3.2, 22.3));
				addToExpectedOutput(new PedestrianIdKey(7), new VPoint(1.2, 3.3));

			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {
				b.clear();
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());

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
					sj.add(Integer.toString(e.getKey().getPedestrianId()))
							.add(Double.toString(e.getValue().x))
							.add(Double.toString(e.getValue().y));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
