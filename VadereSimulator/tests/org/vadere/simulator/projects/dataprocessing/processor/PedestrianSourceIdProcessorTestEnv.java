package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;

public class PedestrianSourceIdProcessorTestEnv extends ProcessorTestEnv<PedestrianIdKey, Integer> {

	PedestrianSourceIdProcessorTestEnv() {
		super(PedestrianSourceIdProcessor.class, PedestrianIdKey.class);
	}

	List<Pedestrian> getPeds(Integer[] ids, Integer[] source) {
		List<Pedestrian> peds = new ArrayList<>();
		for (int i = 0; i < ids.length; i++) {
			Pedestrian p = new Pedestrian(new AttributesAgent(ids[i]), new Random());
			p.setSource(new Source(new AttributesSource(source[i])));
			peds.add(p);
		}
		return peds;
	}

	@Override
	public void loadDefaultSimulationStateMocks() {

		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				List<Pedestrian> pedes = getPeds(new Integer[]{1, 5, 9, 6}, new Integer[]{3, 3, 1, 2});
				pedes.get(3).setSource(null);
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(pedes);

				int step = state.getStep();
				addToExpectedOutput(new PedestrianIdKey(1), 3);
				addToExpectedOutput(new PedestrianIdKey(5), 3);
				addToExpectedOutput(new PedestrianIdKey(9), 1);
				addToExpectedOutput(new PedestrianIdKey(6), -1);
			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {
				List<Pedestrian> pedes = getPeds(new Integer[]{1, 5, 9, 12}, new Integer[]{3, 4, 1, 1});
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(pedes);

				int step = state.getStep();
				addToExpectedOutput(new PedestrianIdKey(1), 3);
				addToExpectedOutput(new PedestrianIdKey(5), 4);
				addToExpectedOutput(new PedestrianIdKey(9), 1);
				addToExpectedOutput(new PedestrianIdKey(12), 1);
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
							.add(Integer.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
