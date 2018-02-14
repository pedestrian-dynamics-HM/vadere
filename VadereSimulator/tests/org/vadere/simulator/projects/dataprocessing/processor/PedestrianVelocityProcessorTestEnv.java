package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.state.attributes.processor.AttributesPedestrianVelocityProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.mockito.Mockito.when;

public class PedestrianVelocityProcessorTestEnv extends  ProcessorTestEnv<TimestepPedestrianIdKey, Double>{

	private DataProcessor pedPosProc;
	private PedestrianPositionProcessorTestEnv pedPosProcEnv;

	PedestrianVelocityProcessorTestEnv(){
		testedProcessor = processorFactory.createDataProcessor(PedestrianVelocityProcessor.class);
		testedProcessor.setId(nextProcessorId());
		AttributesPedestrianVelocityProcessor attr =
				(AttributesPedestrianVelocityProcessor) testedProcessor.getAttributes();
		attr.setPedestrianPositionProcessorId(99);


		pedPosProcEnv = new PedestrianPositionProcessorTestEnv(99);
		pedPosProcEnv.init();
		pedPosProc = pedPosProcEnv.getTestedProcessor();

		outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
				TimestepPedestrianIdKey.class,
				testedProcessor.getId()
		);
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());
		when(manager.getProcessor(99)).thenReturn(pedPosProc);
	}

	PedestrianListBuilder b = new PedestrianListBuilder();
	SimulationStateMock state1;
	SimulationStateMock state2;

	public void loadSimulationStateMocksWithBackstep2(){
		states.clear();
		pedPosProcEnv.getSimStates().clear();

		states.add(state1);

		states.add(state2);

		states.add(new SimulationStateMock(3) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.5, 1.0))
						.add(2, new VPoint(5.3, 6.3))
						.add(3, new VPoint(5.7, 3.3));

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(2.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.5);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 5.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0);


				pedPosProcEnv.addSimState(this);
			}
		});
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		state1 = new SimulationStateMock(1) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.0, 1.0))
						.add(2, new VPoint(2.3, 2.3))
						.add(3, new VPoint(5.7, 3.3));

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(0.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 0.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0);


				pedPosProcEnv.addSimState(this);
			}
		};
		states.add(state1);

		state2 = new SimulationStateMock(2) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.5, 1.0))
						.add(2, new VPoint(5.3, 6.3))
						.add(3, new VPoint(5.7, 3.3));

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(1.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.5);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 5.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0);


				pedPosProcEnv.addSimState(this);
			}
		};
		states.add(state2);

		states.add(new SimulationStateMock(3) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.5, 1.0))
						.add(2, new VPoint(5.3, 6.3))
						.add(3, new VPoint(5.7, 3.3));

				when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				when(state.getSimTimeInSec()).thenReturn(2.0);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 1), 0.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 2), 0.0);
				addToExpectedOutput(new TimestepPedestrianIdKey(step, 3), 0.0);


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
					sj.add(Integer.toString(e.getKey().getTimestep()))
							.add(Integer.toString(e.getKey().getPedestrianId()))
							.add(Double.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
