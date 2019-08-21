package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityCountingProcessor;
import org.vadere.state.attributes.processor.AttributesPedestrianFlowProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static junit.framework.TestCase.fail;

public class PedestrianFlowProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdKey, Double> {

	private PedestrianListBuilder b = new PedestrianListBuilder();
	private DataProcessor pedDensCountProc;

	PedestrianFlowProcessorTestEnv() {
		super(PedestrianFlowProcessor.class, TimestepPedestrianIdKey.class);
	}

	@Override
	void initializeDependencies() {
		int pedDensCountProcId = addDependentProcessor(PedestrianDensityCountingProcessorTestEnv::new);
		pedDensCountProc = getProcessorById(pedDensCountProcId);
		if (pedDensCountProc == null)
			fail("Faild in initializeDependencies: No Processor found for id " + pedDensCountProcId);

		int pedVelProcId = addDependentProcessor(PedestrianVelocityProcessorTestEnv::new);
		AttributesPedestrianFlowProcessor attr =
				(AttributesPedestrianFlowProcessor) testedProcessor.getAttributes();
		attr.setPedestrianDensityProcessorId(pedDensCountProcId);
		attr.setPedestrianVelocityProcessorId(pedVelProcId);

	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		AttributesPedestrianDensityCountingProcessor attr =
				(AttributesPedestrianDensityCountingProcessor) pedDensCountProc.getAttributes();

		double radius = attr.getRadius();
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {

				VPoint p = new VPoint(1.4, 1.4);
				VPoint pPrecise = p.clone().addPrecise(new VPoint(Math.sqrt(radius) - 0.001, Math.sqrt(radius) - 0.001));

				b.clear().add(1, new VPoint(1,1), 0);
				b.add(2, new VPoint(1.2,1.2), 0);
				b.add(3, p, 0);
				b.add(4, pPrecise, 0);
				b.add(5, new VPoint(10,10), 0);

				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getSimTimeInSec()).thenReturn(0.0);

				int step = state.getStep();
				double circleArea = radius * radius * Math.PI;

				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.0 * 3.0 / circleArea);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 0.0 * 3.0 / circleArea);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0 * 4.0 / circleArea);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 4), 0.0 * 2.0 / circleArea);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 5), 0.0 * 1.0 / circleArea);

			}
		});


		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {

				VPoint mov = new VPoint(3.0, 4.0); //dist = 5 time = 1 -> 5ms^-1

				VPoint p = new VPoint(1.4, 1.4);
				VPoint pPrecise = p.clone().addPrecise(new VPoint(Math.sqrt(radius) - 0.001, Math.sqrt(radius) - 0.001));

				b.clear().add(1, new VPoint(1,1).addPrecise(mov), 1);
				b.add(2, new VPoint(1.2,1.2).addPrecise(mov), 1);
				b.add(3, p.addPrecise(mov), 1);
				b.add(4, pPrecise.addPrecise(mov), 1);
				b.add(5, new VPoint(10,10).addPrecise(mov), 1);

				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getSimTimeInSec()).thenReturn(1.0);

				int step = state.getStep();
				double circleArea = radius * radius * Math.PI;
				// parenthesis are needed to ensure the same precision created by the processor.
				// Without parenthesis value for 5.0 * 3.0 / circleArea (radius 2.0) is off by
				// 0.0000000000000001 and the test will do a String compare so it must match exactly
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 5.0 * (3.0 / circleArea));
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 5.0 * (3.0 / circleArea));
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 5.0 * (4.0 / circleArea));
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 4), 5.0 * (2.0 / circleArea));
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 5), 5.0 * (1.0 / circleArea));

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
							.add(Double.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
