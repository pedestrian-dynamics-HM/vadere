package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.datakey.OverlapData;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.state.attributes.processor.AttributesMaxOverlapProcessor;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class MaxOverlapProcessorTestEnv extends ProcessorTestEnv<NoDataKey, Double> {

	private PedestrianListBuilder b = new PedestrianListBuilder();

	MaxOverlapProcessorTestEnv() {
		this(1);
	}

	@SuppressWarnings("unchecked")
	private MaxOverlapProcessorTestEnv(int nextProcessorId) {
		try {
			testedProcessor = processorFactory.createDataProcessor(MaxOverlapProcessor.class);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		testedProcessor.setId(nextProcessorId);
		this.nextProcessorId = nextProcessorId + 1;

		DataProcessor pedestrianOverlapProcessor;
		PedestrianOverlapProcessorTestEnv pedestrianOverlapProcessorTestEnv;
		int pedestrianOverlapProcessorId = nextProcessorId();

		//add ProcessorId of required Processors to current Processor under test
		AttributesMaxOverlapProcessor attr = (AttributesMaxOverlapProcessor) testedProcessor.getAttributes();
		attr.setPedestrianOverlapProcessorId(pedestrianOverlapProcessorId);

		//create required Processor enviroment and add it to current Processor under test
		pedestrianOverlapProcessorTestEnv = new PedestrianOverlapProcessorTestEnv(pedestrianOverlapProcessorId);
		pedestrianOverlapProcessor = pedestrianOverlapProcessorTestEnv.getTestedProcessor();

		Mockito.when(manager.getProcessor(pedestrianOverlapProcessorId)).thenReturn(pedestrianOverlapProcessor);
		addRequiredProcessors(pedestrianOverlapProcessorTestEnv);

		//setup output file with different VadereWriter impl for test
		try {
			outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
					NoDataKey.class,
					testedProcessor.getId()
			);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());

	}

	private LinkedCellsGrid<DynamicElement> getCellGridMock(PedestrianListBuilder b) {
		LinkedCellsGrid<DynamicElement> cellsGrid = new LinkedCellsGrid<>(0.0, 0.0, 10.0, 10.0, 1);
		b.getDynamicElementList().forEach(cellsGrid::addObject);
		return cellsGrid;
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		double minDist = 0.195*2;
		clearStates();


		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				b.clear().add(1, new VPoint(1.0, 1.0));
				b.add(2, new VPoint(1.5, 1.5));
				b.add(3, new VPoint(1.5, 1.0));
				b.add(4, new VPoint(1.0, 1.5));
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getTopography().getSpatialMap(DynamicElement.class)).thenReturn(getCellGridMock(b));
				Mockito.when(state.getTopography().getAttributesPedestrian().getRadius()).thenReturn(0.195);

			}
		});

		addSimState(new SimulationStateMock(2) {
			@Override
			public void mockIt() {

				b.clear().add(1, new VPoint(1.0, 1.0));
				b.add(2, new VPoint(1.5, 1.5));
				b.add(3, new VPoint(1.2, 1.0));
				b.add(4, new VPoint(1.0, 1.5));
				b.add(5, new VPoint(0.8, 0.8));
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getTopography().getSpatialMap(DynamicElement.class)).thenReturn(getCellGridMock(b));
				Mockito.when(state.getTopography().getAttributesPedestrian().getRadius()).thenReturn(0.195);

				OverlapData dist1 = b.overlapData(1,5,minDist);
				OverlapData dist2 = b.overlapData(5,1,minDist);
				OverlapData dist3 = b.overlapData(1,3,minDist);
				OverlapData dist4 = b.overlapData(3,1, minDist);

				double maxDist = 0;
				if (dist1.getOverlap() > maxDist)
					maxDist = dist1.getOverlap();

				if (dist2.getOverlap() > maxDist)
					maxDist = dist2.getOverlap();

				if (dist3.getOverlap() > maxDist)
					maxDist = dist3.getOverlap();

				if (dist4.getOverlap() > maxDist)
					maxDist = dist4.getOverlap();

				addToExpectedOutput(NoDataKey.key(), maxDist);
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
					sj.add(Double.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
