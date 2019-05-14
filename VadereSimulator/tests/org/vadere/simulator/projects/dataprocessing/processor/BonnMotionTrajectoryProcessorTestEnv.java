package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.simulator.projects.dataprocessing.datakey.BonnMotionKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.processor.AttributesBonnMotionTrajectoryProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

public class BonnMotionTrajectoryProcessorTestEnv
		extends ProcessorTestEnv<BonnMotionKey, List<Pair<Double, VPoint>>> {

	private PedestrianListBuilder b = new PedestrianListBuilder();

	public BonnMotionTrajectoryProcessorTestEnv() {
		this(1);
	}

	BonnMotionTrajectoryProcessorTestEnv(int nextProcessorId) {
		super(BonnMotionTrajectoryProcessor.class, BonnMotionKey.class, nextProcessorId);
	}

	@Override
	void initializeDependencies() {
		int pedPosProcessorId = addDependentProcessor(PedestrianPositionProcessorTestEnv::new);
		AttributesBonnMotionTrajectoryProcessor attr =
				(AttributesBonnMotionTrajectoryProcessor) testedProcessor.getAttributes();
		attr.setPedestrianPositionProcessorId(pedPosProcessorId);
	}

	@Override
	public void loadDefaultSimulationStateMocks() {

		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				b.clear()
						.add(1, new VPoint(1.0, 1.0))
						.add(2, new VPoint(3.5, 2.5));
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getScenarioStore().getAttributesSimulation().getSimTimeStepLength()).thenReturn(0.4);
			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {
				b.clear()
						.add(1, new VPoint(2.0, 2.0))
						.add(2, new VPoint(3.5, 3.5));
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getScenarioStore().getAttributesSimulation().getSimTimeStepLength()).thenReturn(0.4);
			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {
				b.clear()
						.add(1, new VPoint(3.0, 3.0))
						.add(2, new VPoint(3.5, 4.5));
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getScenarioStore().getAttributesSimulation().getSimTimeStepLength()).thenReturn(0.4);

			}
		});
	}

	@Override
	List<String> getExpectedOutputAsList() {
		List<String> ret = new ArrayList<>();
		ret.add("0.400000 1.000000 1.000000 0.800000 2.000000 2.000000 1.200000 3.000000 3.000000");
		ret.add("0.400000 3.500000 2.500000 0.800000 3.500000 3.500000 1.200000 3.500000 4.500000");
		return ret;
	}
}
