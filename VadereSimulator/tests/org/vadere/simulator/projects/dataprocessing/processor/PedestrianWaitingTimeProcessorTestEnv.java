package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.processor.AttributesPedestrianWaitingTimeProcessor;
import org.vadere.state.attributes.scenario.AttributesMeasurementArea;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PedestrianWaitingTimeProcessorTestEnv extends ProcessorTestEnv<PedestrianIdKey, Double> {

	private PedestrianListBuilder b = new PedestrianListBuilder();

	PedestrianWaitingTimeProcessorTestEnv() {
		super(PedestrianWaitingTimeProcessor.class, PedestrianIdKey.class);
	}

	@Override
	void initializeDependencies() {
		AttributesPedestrianWaitingTimeProcessor attr =
				(AttributesPedestrianWaitingTimeProcessor) testedProcessor.getAttributes();

		attr.setWaitingAreaId(42);
		MeasurementArea measurementArea = new MeasurementArea(
				new AttributesMeasurementArea(42, new VRectangle(0, 0, 2, 5)));
		Mockito.when(manager.getMeasurementArea(42, true)).thenReturn(measurementArea);
	}

	@Override
	public void loadDefaultSimulationStateMocks() {

		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				b.clear().add(1, new VPoint(0.0, 1.0));
				b.add(2, new VPoint(1.0, 2.0));
				b.add(3, new VPoint(1.5, 3.0));
				b.add(4, new VPoint(0.0, 7.0));  //never inside

				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getSimTimeInSec()).thenReturn(0.0);

				addToExpectedOutput(new PedestrianIdKey(1), 0.0);
				addToExpectedOutput(new PedestrianIdKey(2), 0.0);
				addToExpectedOutput(new PedestrianIdKey(3), 0.0);

			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {
				b.clear().add(1, new VPoint(0.5, 1.0));
				b.add(2, new VPoint(1.5, 2.0));
				b.add(3, new VPoint(2.1, 3.0));    //leave area
				b.add(4, new VPoint(0.5, 7.0));   //never inside

				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getSimTimeInSec()).thenReturn(0.8);

				addToExpectedOutput(new PedestrianIdKey(1), 0.8);
				addToExpectedOutput(new PedestrianIdKey(2), 0.8);
			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {
				b.clear().add(1, new VPoint(1.0, 1.0));
				b.add(2, new VPoint(2.2, 2.0));    //leave area
				b.add(3, new VPoint(2.8, 3.0));
				b.add(4, new VPoint(1.5, 7.0));   //never inside

				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getSimTimeInSec()).thenReturn(1.7);

				addToExpectedOutput(new PedestrianIdKey(1), 1.7);
			}
		});

		addSimState(new SimulationStateMock(4) {
			@Override
			public void mockIt() {
				b.clear().add(1, new VPoint(2.1, 1.0));    //leave area
				b.add(2, new VPoint(3.2, 2.0));
				b.add(3, new VPoint(3.8, 3.0));
				b.add(4, new VPoint(5.5, 7.0));   //never inside

				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getSimTimeInSec()).thenReturn(3.6);
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
