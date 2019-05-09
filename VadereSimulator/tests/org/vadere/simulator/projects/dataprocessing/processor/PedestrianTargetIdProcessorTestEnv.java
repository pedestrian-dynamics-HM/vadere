package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;

public class PedestrianTargetIdProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdKey, Integer> {

	PedestrianTargetIdProcessorTestEnv() {
		super(PedestrianTargetIdProcessor.class, TimestepPedestrianIdKey.class);
	}

	List<Pedestrian> getPeds(Integer[] ids, Integer[] targets) {
		List<Pedestrian> peds = new ArrayList<>();
		for (int i = 0; i < ids.length; i++) {
			Pedestrian p = new Pedestrian(new AttributesAgent(ids[i]), new Random());
			p.getTargets().add(targets[i]);
			peds.add(p);
		}
		return peds;
	}


	@Override
	public void loadDefaultSimulationStateMocks() {
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = getPeds(new Integer[]{12, 33, 40, 2}, new Integer[]{1, 1, 2, 2});
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(peds);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 12), 1);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 33), 1);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 40), 2);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 2);
			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = getPeds(new Integer[]{12, 33, 40, 9}, new Integer[]{1, 2, 3, 1});
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(peds);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 12), 1);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 33), 2);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 40), 3);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 9), 1);
			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = getPeds(new Integer[]{10, 9}, new Integer[]{1, 1});
				peds.get(0).getTargets().clear();
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(peds);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 10), -1);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 9), 1);
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
							.add(Integer.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
