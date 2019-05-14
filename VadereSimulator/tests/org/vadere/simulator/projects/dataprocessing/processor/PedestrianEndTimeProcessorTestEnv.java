package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;

public class PedestrianEndTimeProcessorTestEnv extends ProcessorTestEnv<PedestrianIdKey, Double> {

	PedestrianEndTimeProcessorTestEnv() {
		super(PedestrianEndTimeProcessor.class, PedestrianIdKey.class);
	}

	List<Pedestrian> getPeds(Integer[] ids) {
		List<Pedestrian> peds = new ArrayList<>();
		for (Integer id : ids) {
			Pedestrian p = new Pedestrian(new AttributesAgent(id), new Random());
			peds.add(p);
		}
		return peds;
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = getPeds(new Integer[]{3, 4, 7, 6, 8});
				Mockito.when(state.getSimTimeInSec()).thenReturn(0.4);
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(peds);

				double time = state.getSimTimeInSec();
				addToExpectedOutput(new PedestrianIdKey(3), time);
				addToExpectedOutput(new PedestrianIdKey(4), time);
				addToExpectedOutput(new PedestrianIdKey(7), time);
				addToExpectedOutput(new PedestrianIdKey(8), time);
			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = getPeds(new Integer[]{3, 4, 7, 6, 8, 10, 12});
				Mockito.when(state.getSimTimeInSec()).thenReturn(1.2);
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(peds);

				double time = state.getSimTimeInSec();
				addToExpectedOutput(new PedestrianIdKey(3), time);
				addToExpectedOutput(new PedestrianIdKey(4), time);
				addToExpectedOutput(new PedestrianIdKey(7), time);
				addToExpectedOutput(new PedestrianIdKey(6), time);
				addToExpectedOutput(new PedestrianIdKey(8), time);
				addToExpectedOutput(new PedestrianIdKey(10), time);
				addToExpectedOutput(new PedestrianIdKey(12), time);
			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = getPeds(new Integer[]{3, 6, 8, 10, 12});
				Mockito.when(state.getSimTimeInSec()).thenReturn(1.2);
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(peds);

				double time = state.getSimTimeInSec();
				addToExpectedOutput(new PedestrianIdKey(3), time);
				addToExpectedOutput(new PedestrianIdKey(8), time);
				addToExpectedOutput(new PedestrianIdKey(10), time);
				addToExpectedOutput(new PedestrianIdKey(12), time);
			}
		});

		addSimState(new SimulationStateMock(4) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = getPeds(new Integer[]{8, 10});
				Mockito.when(state.getSimTimeInSec()).thenReturn(1.2);
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(peds);

				double time = state.getSimTimeInSec();
				addToExpectedOutput(new PedestrianIdKey(8), Double.POSITIVE_INFINITY);
				addToExpectedOutput(new PedestrianIdKey(10), Double.POSITIVE_INFINITY);
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
							.add(Double.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
