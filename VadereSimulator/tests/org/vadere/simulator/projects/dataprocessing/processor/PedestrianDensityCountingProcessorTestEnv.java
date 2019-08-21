package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityCountingProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PedestrianDensityCountingProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdKey, Double> {

	private PedestrianListBuilder b = new PedestrianListBuilder();

	PedestrianDensityCountingProcessorTestEnv() {
		this(1);
	}

	PedestrianDensityCountingProcessorTestEnv(int nextProcessorId) {
		super(PedestrianDensityCountingProcessor.class, TimestepPedestrianIdKey.class, nextProcessorId);
	}

	@Override
	void initializeDependencies() {
		int pedPosProcId = addDependentProcessor(PedestrianPositionProcessorTestEnv::new);
		AttributesPedestrianDensityCountingProcessor attr =
				(AttributesPedestrianDensityCountingProcessor) testedProcessor.getAttributes();
		attr.setPedestrianPositionProcessorId(pedPosProcId);
		attr.setRadius(2.0);
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		AttributesPedestrianDensityCountingProcessor attr =
				(AttributesPedestrianDensityCountingProcessor) testedProcessor.getAttributes();
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

				int step = state.getStep();
				double circleArea = radius * radius * Math.PI;
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 3.0 / circleArea);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 3.0 / circleArea);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 4.0 / circleArea);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 4), 2.0 / circleArea);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 5), 1.0 / circleArea);
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
