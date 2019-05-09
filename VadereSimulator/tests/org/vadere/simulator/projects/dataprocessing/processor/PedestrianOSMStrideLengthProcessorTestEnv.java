package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.mockito.Mockito;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.sfm.SocialForceModel;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PedestrianOSMStrideLengthProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdKey, Double> {

	PedestrianOSMStrideLengthProcessorTestEnv() {
		super(PedestrianOSMStrideLengthProcessor.class, TimestepPedestrianIdKey.class);
	}

	public void loadWrongModel() {
		MainModel model = mock(SocialForceModel.class, Mockito.RETURNS_DEEP_STUBS);
		when(manager.getMainModel()).thenReturn(model);
		clearStates();
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		MainModel model = mock(OptimalStepsModel.class, Mockito.RETURNS_DEEP_STUBS);
		when(manager.getMainModel()).thenReturn(model);

		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = new LinkedList<>();
				peds.add(getPedMock(1, 0.456));
				peds.add(getPedMock(2, 0.321));
				peds.add(getPedMock(3, 0.765));
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(peds);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.456);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 0.321);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.765);
			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = new LinkedList<>();
				peds.add(getPedMock(1, 0.456, 0.333));
				peds.add(getPedMock(2, 0.321, 0.444));
				peds.add(getPedMock(3, 0.765, 0.555));
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(peds);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.333);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 0.444);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.555);
			}
		});

	}

	private PedestrianOSM getPedMock(int id, Double... strides) {
		PedestrianOSM ped = mock(PedestrianOSM.class, Mockito.RETURNS_DEEP_STUBS);
		when(ped.getId()).thenReturn(id);
		LinkedList<Pair<Double, Double>> strideList = new LinkedList<>();

		Arrays.stream(strides).forEach(stide -> strideList.add(Pair.of(stide, 0.2)));
		when(ped.getStrides()).thenReturn(strideList);
		return ped;
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
