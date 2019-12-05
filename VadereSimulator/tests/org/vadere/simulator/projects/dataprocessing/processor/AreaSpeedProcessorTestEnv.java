package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.processor.AttributesAreaSpeedProcessor;
import org.vadere.state.attributes.scenario.AttributesMeasurementArea;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.mockito.Mockito.when;

public class AreaSpeedProcessorTestEnv extends ProcessorTestEnv<TimestepKey, Double> {

	private PedestrianListBuilder b = new PedestrianListBuilder();

	AreaSpeedProcessorTestEnv() {
		super(AreaSpeedProcessor.class, TimestepKey.class);
	}

	@Override
	void initializeDependencies() {
		AttributesAreaSpeedProcessor attr =
				(AttributesAreaSpeedProcessor) testedProcessor.getAttributes();

		int pedPosProcId = addDependentProcessor(PedestrianPositionProcessorTestEnv::new);
		int pedVelProcId = addDependentProcessor(PedestrianVelocityProcessorTestEnv::new);

		attr.setPedestrianPositionProcessorId(pedPosProcId);
		attr.setPedestrianVelocityProcessorId(pedVelProcId);

		attr.setMeasurementAreaId(99);
		MeasurementArea measurementArea = new MeasurementArea(
				new AttributesMeasurementArea(99, new VRectangle(0, 0, 4, 5)));
		Mockito.when(manager.getMeasurementArea(99, false)).thenReturn(measurementArea);
	}

	@Override
	public void loadDefaultSimulationStateMocks() {

		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1,0), 0)
						.add(2, new VPoint(0,0), 0)
						.add(3, new VPoint(7,4), 0);    //not in area

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(0.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepKey(step), 0.0);
			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1,1), 1)   //dist = 1.0
						.add(2, new VPoint(3,4), 1)    //dist = 5.0
						.add(3, new VPoint(8,4), 1);   //not in area

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				double simTime = 1.0;
				when(state.getSimTimeInSec()).thenReturn(simTime);

				int step = state.getStep();
				double time = simTime - 0.0;
				double areaSpeed = (1.0 / time + 5.0 / time) / 2; // 2 = noOfPeds
				addToExpectedOutput(new TimestepKey(step), areaSpeed);
			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(3,1), 2)   //dist = 2.0
						.add(2, new VPoint(5,4), 2)    //not in area
						.add(3, new VPoint(8,8), 2);   //not in area

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				double simTime = 2.0;
				when(state.getSimTimeInSec()).thenReturn(simTime);

				int step = state.getStep();
				double time = simTime - 1.0; // time in this step
				double areaSpeed = (2.0 / time) / 1; // 1 = noOfPeds
				addToExpectedOutput(new TimestepKey(step), areaSpeed);
			}
		});

		addSimState(new SimulationStateMock(4) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(6,1), 3)   // not in area
						.add(2, new VPoint(7,4), 3)    // not in area
						.add(3, new VPoint(9,8), 3);   // not in area

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				double simTime = 3.0;
				when(state.getSimTimeInSec()).thenReturn(simTime);

				int step = state.getStep();

				// if no agent is in the area, then return NaN (see #287)
				addToExpectedOutput(new TimestepKey(step), Double.NaN);
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
							.add(Double.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
