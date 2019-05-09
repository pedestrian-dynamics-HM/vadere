package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;

import static org.mockito.Mockito.when;

public class PedestrianStateProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdKey, String> {

	private Pedestrian[] peds;

	PedestrianStateProcessorTestEnv() {
		super(PedestrianStateProcessor.class, TimestepPedestrianIdKey.class);
	}

	private List<Pedestrian> getPeds(Integer... ids) {
		List<Pedestrian> selPeds = new ArrayList<>();
		for (Integer id : ids) {
			selPeds.add(peds[id]);
		}
		return selPeds;
	}

	private void addToExpectedOutput(int step, List<Pedestrian> peds, String value) {
		peds.forEach(p -> super.addToExpectedOutput(
				new TimestepPedestrianIdKey(step, p.getId()), value));
	}

	@Override
	public void loadDefaultSimulationStateMocks() {

		peds = new Pedestrian[10];
		for (int id = 0; id < 10; id++) {
			peds[id] = new Pedestrian(new AttributesAgent(id), new Random());
		}

		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = getPeds(1, 2, 3, 4, 5);
				when(state.getTopography().getElements(Pedestrian.class).stream())
						.thenReturn(peds.stream());

				addToExpectedOutput(state.getStep(), peds, "c");
			}
		});


		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = getPeds(1, 2, 3, 4, 5, 7, 8);
				when(state.getTopography().getElements(Pedestrian.class).stream())
						.thenReturn(peds.stream());

				addToExpectedOutput(state.getStep(), getPeds(7, 8), "c");
				addToExpectedOutput(state.getStep(), getPeds(4, 5), "m");
			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {
				List<Pedestrian> peds = getPeds(4, 5, 7, 8, 9);
				when(state.getTopography().getElements(Pedestrian.class).stream())
						.thenReturn(peds.stream());

				addToExpectedOutput(state.getStep(), getPeds(9), "c");
				addToExpectedOutput(state.getStep(), getPeds(4, 5, 7, 8), "m");
				addToExpectedOutput(state.getStep() - 1, getPeds(1, 2, 3), "d");
			}
		});

	}

	@Override
	List<String> getExpectedOutputAsList() {
		List<String> outputList = new ArrayList<>();
		//sort by Outputfile Key and create one list element for each row.
		expectedOutput.entrySet()
				.stream()
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.forEach(e -> {
					StringJoiner sj = new StringJoiner(getDelimiter());
					sj.add(Integer.toString(e.getKey().getTimestep()))
							.add(Integer.toString(e.getKey().getPedestrianId()))
							.add(e.getValue());
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
