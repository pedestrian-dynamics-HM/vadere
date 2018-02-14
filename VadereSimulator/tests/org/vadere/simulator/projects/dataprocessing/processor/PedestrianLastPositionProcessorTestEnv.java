package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.state.attributes.processor.AttributesPedestrianLastPositionProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.mockito.Mockito.when;

public class PedestrianLastPositionProcessorTestEnv extends ProcessorTestEnv<PedestrianIdKey, VPoint> {

	PedestrianListBuilder b = new PedestrianListBuilder();
	private int pedPosProcId;
	private DataProcessor pedPosProc;
	private PedestrianPositionProcessorTestEnv pedPosProcEnv;

	PedestrianLastPositionProcessorTestEnv() {

		testedProcessor = processorFactory.createDataProcessor(PedestrianLastPositionProcessor.class);
		testedProcessor.setId(nextProcessorId());
		((AttributesPedestrianLastPositionProcessor) testedProcessor.getAttributes())
				.setPedestrianPositionProcessorId(99);
		pedPosProcId = 99;

		pedPosProcEnv = new PedestrianPositionProcessorTestEnv(pedPosProcId);
		pedPosProcEnv.init();
		pedPosProc = pedPosProcEnv.getTestedProcessor();

		outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
				PedestrianIdKey.class,
				testedProcessor.getId()
		);
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());
		when(manager.getProcessor(pedPosProcId)).thenReturn(pedPosProc);
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		states.add(new SimulationStateMock(1) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.0, 1.2))
						.add(3, new VPoint(4.45, 1.2))
						.add(5, new VPoint(3.546, 7.2342));
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());

				addToExpectedOutput(new PedestrianIdKey(1), new VPoint(1.0, 1.2));
				addToExpectedOutput(new PedestrianIdKey(3), new VPoint(4.45, 1.2));
				addToExpectedOutput(new PedestrianIdKey(5), new VPoint(3.546, 7.2342));

				//also set this SimulstaionStateMock for all dependencies.
				pedPosProcEnv.addSimState(this);
			}
		});

		states.add(new SimulationStateMock(2) {
			@Override
			public void mockIt() {
				b.clear().add(1, new VPoint(33.2, 3.22))
						.add(3, new VPoint(3.2, 22.3))
						.add(7, new VPoint(1.2, 3.3));
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());

				//overwrite Values!
				addToExpectedOutput(new PedestrianIdKey(1), new VPoint(33.2, 3.22));
				addToExpectedOutput(new PedestrianIdKey(3), new VPoint(3.2, 22.3));
				addToExpectedOutput(new PedestrianIdKey(7), new VPoint(1.2, 3.3));

				pedPosProcEnv.addSimState(this);
			}
		});

		states.add(new SimulationStateMock(3) {
			@Override
			public void mockIt() {
				b.clear();
				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());

				//no Pedestrians left so nothing to add in this step.
				pedPosProcEnv.addSimState(this);
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
							.add(Double.toString(e.getValue().x))
							.add(Double.toString(e.getValue().y));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
