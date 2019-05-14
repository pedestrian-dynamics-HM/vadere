package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianWaitingEndTimeProcessor;
import org.vadere.state.attributes.scenario.AttributesMeasurementArea;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PedestrianWaitingEndTimeProcessorTestEnv extends ProcessorTestEnv<PedestrianIdKey, Double> {

	PedestrianWaitingEndTimeProcessorTestEnv() {
		super(PedestrianWaitingEndTimeProcessor.class, PedestrianIdKey.class);
	}

	@Override
	void initializeDependencies() {
		AttributesPedestrianWaitingEndTimeProcessor attr =
				(AttributesPedestrianWaitingEndTimeProcessor) testedProcessor.getAttributes();
		attr.setWaitingAreaId(42);
		MeasurementArea measurementArea = new MeasurementArea(
				new AttributesMeasurementArea(42, new VRectangle(0, 0, 16, 16)));
		Mockito.when(manager.getMeasurementArea(42, true)).thenReturn(measurementArea);
	}

	@Override
	public void loadDefaultSimulationStateMocks() {

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
