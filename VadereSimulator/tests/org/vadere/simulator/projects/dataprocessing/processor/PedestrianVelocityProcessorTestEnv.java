package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.processor.AttributesPedestrianVelocityProcessor;
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

public class PedestrianVelocityProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdKey, Double> {

	private PedestrianListBuilder b = new PedestrianListBuilder();
	private SimulationStateMock state1;
	private SimulationStateMock state2;

	PedestrianVelocityProcessorTestEnv() {
		this(1);
	}

	PedestrianVelocityProcessorTestEnv(int nextProcessorId) {
		super(PedestrianVelocityProcessor.class, TimestepPedestrianIdKey.class, nextProcessorId);
	}

	@Override
	void initializeDependencies() {
		int pedPosProcId = addDependentProcessor(PedestrianPositionProcessorTestEnv::new);
		//add ProcessorId of required Processors to current Processor under test
		AttributesPedestrianVelocityProcessor attr =
				(AttributesPedestrianVelocityProcessor) testedProcessor.getAttributes();
		attr.setPedestrianPositionProcessorId(pedPosProcId);
	}

	void loadSimulationStateMocksWithBackstep2() {
		clearStates();

		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1,1), 0)
						.add(2, new VPoint(2.3,2.3), 0)
						.add(3, new VPoint(5.7,3.3), 0);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(0.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 0.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0);

			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.5,1.0), 1)
						.add(2, new VPoint(5.3,6.3), 1)
						.add(3, new VPoint(5.7,3.3), 1);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(1.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.5);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 5.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0);

			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.5,1.0), 2)
						.add(2, new VPoint(5.3,6.3), 2)
						.add(3, new VPoint(5.7,3.3), 2);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(2.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.25);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 2.5);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0);

			}
		});
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		state1 = new SimulationStateMock(1) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.0,1.0), 0)
						.add(2, new VPoint(2.3,2.3), 0)
						.add(3, new VPoint(5.7,3.3), 0);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(0.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 0.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0);

			}
		};
		addSimState(state1);

		state2 = new SimulationStateMock(2) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.5,1.0), 1)
						.add(2, new VPoint(5.3,6.3), 1)
						.add(3, new VPoint(5.7,3.3), 1);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(1.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.5);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 5.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0);

			}
		};
		addSimState(state2);

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.5,1.0), 2)
						.add(2, new VPoint(5.3,6.3), 2)
						.add(3, new VPoint(5.7,3.3), 2);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(2.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 0.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0);

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
							.add(Double.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
