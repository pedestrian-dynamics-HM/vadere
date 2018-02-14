package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.tests.reflection.ReflectionHelper;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PedestrianWaitingTimeProcessorTestEnv extends ProcessorTestEnv<PedestrianIdKey, Double> {

	PedestrianWaitingTimeProcessorTestEnv() {
		testedProcessor = processorFactory.createDataProcessor(PedestrianWaitingTimeProcessor.class);
		testedProcessor.setId(nextProcessorId());

		outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
				PedestrianIdKey.class,
				testedProcessor.getId());
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());
	}


	@Override
	public void loadDefaultSimulationStateMocks(){

		//VRectangle(0, 0, 2, 5)

		/**
		 * Ped 1,2,3 start within WaitingArea and 4 never enters Area.
		 */
		states.add(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				Map<Integer, VPoint> pedPosMap = new HashMap<>();
				pedPosMap.put(1, new VPoint(0.0, 1.0));
				pedPosMap.put(2, new VPoint(1.0, 2.0));
				pedPosMap.put(3, new VPoint(1.5, 3.0));
				pedPosMap.put(4, new VPoint(0.0, 7.0));  //never inside

				Mockito.when(state.getPedestrianPositionMap()).thenReturn(pedPosMap);
				Mockito.when(state.getSimTimeInSec()).thenReturn(0.0);

				addToExpectedOutput(new PedestrianIdKey(1), 0.0);
				addToExpectedOutput(new PedestrianIdKey(2), 0.0);
				addToExpectedOutput(new PedestrianIdKey(3), 0.0);

			}
		});

		states.add(new SimulationStateMock(2) {
			@Override
			public void mockIt() {
				Map<Integer, VPoint> pedPosMap = new HashMap<>();
				pedPosMap.put(1, new VPoint(0.5, 1.0));
				pedPosMap.put(2, new VPoint(1.5, 2.0));
				pedPosMap.put(3, new VPoint(2.1, 3.0));	//leave area
				pedPosMap.put(4, new VPoint(0.5, 7.0));   //never inside

				Mockito.when(state.getPedestrianPositionMap()).thenReturn(pedPosMap);
				Mockito.when(state.getSimTimeInSec()).thenReturn(0.8);

				addToExpectedOutput(new PedestrianIdKey(1), 0.8);
				addToExpectedOutput(new PedestrianIdKey(2), 0.8);
			}
		});

		states.add(new SimulationStateMock(3) {
			@Override
			public void mockIt() {
				Map<Integer, VPoint> pedPosMap = new HashMap<>();
				pedPosMap.put(1, new VPoint(1.0, 1.0));
				pedPosMap.put(2, new VPoint(2.2, 2.0));	//leave area
				pedPosMap.put(3, new VPoint(2.8, 3.0));
				pedPosMap.put(4, new VPoint(1.5, 7.0));   //never inside

				Mockito.when(state.getPedestrianPositionMap()).thenReturn(pedPosMap);
				Mockito.when(state.getSimTimeInSec()).thenReturn(1.7);

				addToExpectedOutput(new PedestrianIdKey(1), 1.7);
			}
		});

		states.add(new SimulationStateMock(4) {
			@Override
			public void mockIt() {
				Map<Integer, VPoint> pedPosMap = new HashMap<>();
				pedPosMap.put(1, new VPoint(2.1, 1.0));	//leave area
				pedPosMap.put(2, new VPoint(3.2, 2.0));
				pedPosMap.put(3, new VPoint(3.8, 3.0));
				pedPosMap.put(4, new VPoint(5.5, 7.0));   //never inside

				Mockito.when(state.getPedestrianPositionMap()).thenReturn(pedPosMap);
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
