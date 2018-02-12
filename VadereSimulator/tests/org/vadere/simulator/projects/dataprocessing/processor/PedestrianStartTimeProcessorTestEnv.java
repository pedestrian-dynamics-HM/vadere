package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.writer.VadereStringWriter;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.PedestrianIdOutputFile;
import org.vadere.simulator.projects.dataprocessing.writer.VadereStringWriterFactory;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;

import static org.mockito.Mockito.when;

/**
 * Test environment for {@link PedestrianStartTimeProcessor}. Define {@link DataProcessor}, {@link
 * OutputFile}  and {@link SimulationStateMock}s
 *
 * @author Stefan Schuhbäck
 */
public class PedestrianStartTimeProcessorTestEnv extends ProcessorTestEnv<PedestrianIdKey, Double> {

	PedestrianStartTimeProcessorTestEnv() {
		super();

		testedProcessor = processorFactory.createDataProcessor(PedestrianStartTimeProcessor.class);
		testedProcessor.setId(nextProcessorId++);

		outputFile = outputFileFactory.createOutputfile(
				PedestrianIdOutputFile.class,
				testedProcessor.getId());
		outputFile.setVadereWriterFactory(new VadereStringWriterFactory());
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		states.add(new SimulationStateMock() {
			@Override
			public void mockIt() {

				List<Pedestrian> m = new ArrayList<>();
				m.add(new Pedestrian(new AttributesAgent(1), new Random()));
				m.add(new Pedestrian(new AttributesAgent(2), new Random()));

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(m);
				when(state.getSimTimeInSec()).thenReturn(0.4);
				when(state.getStep()).thenReturn(1);

				addToExpectedOutput(1, 0.4);
				addToExpectedOutput(2, 0.4);
			}
		});

		states.add(new SimulationStateMock() {
			@Override
			public void mockIt() {

				List<Pedestrian> m = new ArrayList<>();
				m.add(new Pedestrian(new AttributesAgent(1), new Random()));
				m.add(new Pedestrian(new AttributesAgent(2), new Random()));
				m.add(new Pedestrian(new AttributesAgent(3), new Random()));


				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(m);
				when(state.getSimTimeInSec()).thenReturn(1.3);
				when(state.getStep()).thenReturn(2);

				addToExpectedOutput(3, 1.3);
			}
		});

		states.add(new SimulationStateMock() {
			@Override
			public void mockIt() {

				List<Pedestrian> m = new ArrayList<>();
				m.add(new Pedestrian(new AttributesAgent(3), new Random()));
				m.add(new Pedestrian(new AttributesAgent(4), new Random()));

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(m);
				when(state.getSimTimeInSec()).thenReturn(4.7);
				when(state.getStep()).thenReturn(3);

				addToExpectedOutput(4, 4.7);
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

	private void addToExpectedOutput(int id, double time) {
		super.addToExpectedOutput(new PedestrianIdKey(id), time);
	}

}
