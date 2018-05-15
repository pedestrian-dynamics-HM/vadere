package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityCountingProcessor;
import org.vadere.state.scenario.Pedestrian;
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

	@SuppressWarnings("unchecked")
	PedestrianDensityCountingProcessorTestEnv(int nextProcessorId) {
		try {
			testedProcessor = processorFactory.createDataProcessor(PedestrianDensityCountingProcessor.class);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		testedProcessor.setAttributes(new AttributesPedestrianDensityCountingProcessor());
		testedProcessor.setId(nextProcessorId);
		this.nextProcessorId = nextProcessorId + 1;

		int pedPosProcId = nextProcessorId();
		AttributesPedestrianDensityCountingProcessor attr =
				(AttributesPedestrianDensityCountingProcessor) testedProcessor.getAttributes();
		attr.setPedestrianPositionProcessorId(pedPosProcId);
		attr.setRadius(2.0);

		PedestrianPositionProcessorTestEnv pedPosProcEnv = new PedestrianPositionProcessorTestEnv(pedPosProcId);
		DataProcessor pedPosProc = pedPosProcEnv.getTestedProcessor();
		addRequiredProcessors(pedPosProcEnv);
		Mockito.when(manager.getProcessor(pedPosProcId)).thenReturn(pedPosProc);

		try {
			outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
					TimestepPedestrianIdKey.class,
					testedProcessor.getId()
			);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());

	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		AttributesPedestrianDensityCountingProcessor attr =
				(AttributesPedestrianDensityCountingProcessor) testedProcessor.getAttributes();
		double radius = attr.getRadius();
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.0, 1.0));
				b.add(2, new VPoint(1.2, 1.2));
				b.add(3, new VPoint(1.4, 1.4));
				VPoint p = new VPoint(1.4, 1.4);
				p = p.addPrecise(new VPoint(Math.sqrt(radius) - 0.001, Math.sqrt(radius) - 0.001));
				b.add(4, p);
				b.add(5, new VPoint(10.0, 10.0));
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
