package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
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

public class PedestrianPositionProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdKey, VPoint> {

	PedestrianListBuilder b = new PedestrianListBuilder();

	PedestrianPositionProcessorTestEnv() {
		this(1);
	}

	PedestrianPositionProcessorTestEnv(int processorId) {
		super(PedestrianPositionProcessor.class, TimestepPedestrianIdKey.class, processorId);
	}

	private void addToExpectedOutput(Integer step, List<Pedestrian> m) {
		for (Pedestrian p : m) {
			addToExpectedOutput(new TimestepPedestrianIdKey(step, p.getId()), p.getPosition());
		}
	}

	@Override
	public void loadDefaultSimulationStateMocks() {

		addSimState(new SimulationStateMock() {
			@Override
			public void mockIt() {
				when(state.getStep()).thenReturn(1);

				VTrajectory traj1 = new VTrajectory();
				VTrajectory traj2 = new VTrajectory();
				VTrajectory traj3 = new VTrajectory();

				traj1.add(new FootStep(new VPoint(0,0 ), new VPoint(0,1), 0, 0.4));
				traj2.add(new FootStep(new VPoint(0,0 ), new VPoint(1,0), 0, 0.2));
				traj3.add(new FootStep(new VPoint(0,0 ), new VPoint(1,1), 0, 1.));

				b.clear().add(1, traj1)
						.add(2, traj2)
						.add(3, traj3);

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				addToExpectedOutput(state.getStep(), b.getList());

			}
		});

		addSimState(new SimulationStateMock() {
			@Override
			public void mockIt() {
				when(state.getStep()).thenReturn(2);

                VTrajectory traj1 = new VTrajectory();
                VTrajectory traj2 = new VTrajectory();
                VTrajectory traj3 = new VTrajectory();

                traj1.add(new FootStep(new VPoint(0,0 ), new VPoint(0,1), 0, 0.4));
                traj2.add(new FootStep(new VPoint(0,0 ), new VPoint(1,0), 0, 0.2));
                traj3.add(new FootStep(new VPoint(0,0 ), new VPoint(1,1), 0, 1.));

				b.clear().add(4, traj1)
						.add(5, traj2)
						.add(6, traj3);
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				addToExpectedOutput(state.getStep(), b.getList());
			}
		});

		addSimState(new SimulationStateMock() {
			@Override
			public void mockIt() {
				when(state.getStep()).thenReturn(3);

                VTrajectory traj1 = new VTrajectory();
                traj1.add(new FootStep(new VPoint(0,0 ), new VPoint(1,1), 0, 1.));

				b.clear().add(7, traj1);
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
