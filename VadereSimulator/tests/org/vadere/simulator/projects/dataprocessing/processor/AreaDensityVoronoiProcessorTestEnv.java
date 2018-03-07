package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.state.attributes.processor.AttributesAreaDensityVoronoiProcessor;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.mockito.Mockito.when;

public class AreaDensityVoronoiProcessorTestEnv extends ProcessorTestEnv<TimestepKey, Double> {

	PedestrianListBuilder b = new PedestrianListBuilder();

	AreaDensityVoronoiProcessorTestEnv() {
		testedProcessor = processorFactory.createDataProcessor(AreaDensityVoronoiProcessor.class);
		testedProcessor.setId(nextProcessorId());
		AttributesAreaDensityVoronoiProcessor attr =
				(AttributesAreaDensityVoronoiProcessor) testedProcessor.getAttributes();
		attr.setVoronoiArea(new VRectangle(0, 0, 16, 16));
		attr.setMeasurementArea(new VRectangle(0, 0, 16, 16));

		outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
				TimestepKey.class,
				testedProcessor.getId()
		);
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());
	}

	public void loadCollinearSetup() {
		clearStates();
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				b.clear().add(1, new VPoint(1.0, 1.0));
				b.add(2, new VPoint(2.0, 2.0));
				b.add(3, new VPoint(3.0, 3.0));
				b.add(4, new VPoint(4.0, 4.0));

				when(state.getTopography().getElements(Agent.class)).thenReturn(b.getAgentList());

				int step = state.getStep();
				addToExpectedOutput(new TimestepKey(step), 4.0 / (16.0 * 16.0));
			}
		});

	}

	public void loadOneCircleEvent() {
		clearStates();
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				b.clear().add(1, new VPoint(3.0, 6.5));
				b.add(2, new VPoint(6.0, 8.0));
				VPoint m = new VPoint(6.0, 4.0); //circle mid
				b.add(3, m.add(new VPoint(2.5, 3.0)));

				when(state.getTopography().getElements(Agent.class)).thenReturn(b.getAgentList());

				int step = state.getStep();
				addToExpectedOutput(new TimestepKey(step), 3.0 / (16.0 * 16.0));

			}
		});
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		clearStates();
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
							.add(Double.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
