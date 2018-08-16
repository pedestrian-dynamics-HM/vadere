package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdOverlap;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.mockito.Mockito.mock;

public class PedestrianOverlapProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdOverlap, Double> {

	private PedestrianListBuilder b = new PedestrianListBuilder();

	PedestrianOverlapProcessorTestEnv() {
		try {
			testedProcessor = processorFactory.createDataProcessor(PedestrianOverlapProcessor.class);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		testedProcessor.setId(nextProcessorId());

		try {
			outputFile = outputFileFactory.createDefaultOutputfileByDataKey(
					TimestepPedestrianIdOverlap.class,
					testedProcessor.getId());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		outputFile.setVadereWriterFactory(VadereWriterFactory.getStringWriterFactory());
	}

	LinkedCellsGrid<DynamicElement> getCellGridMock(PedestrianListBuilder b){
		LinkedCellsGrid<DynamicElement> cellsGrid = new LinkedCellsGrid<>(0.0,0.0, 10.0, 10.0, 1);
		b.getDynamicElementList().forEach(e -> cellsGrid.addObject(e));
		return  cellsGrid;
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
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

				int step = state.getStep();
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

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 1, 5), b.getDistByPedId(1,5));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 5, 1), b.getDistByPedId(5,1));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 1, 3), b.getDistByPedId(1, 3));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 3, 1), b.getDistByPedId(3, 1));
			}
		});

		addSimState(new SimulationStateMock(3) {
			@Override
			public void mockIt() {
				b.clear().add(1, new VPoint(1.0, 1.0));
				b.add(2, new VPoint(1.01, 1.0));
				b.add(3, new VPoint(1.0, 1.01));
				b.add(4, new VPoint(1.01, 1.01));
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getTopography().getSpatialMap(DynamicElement.class)).thenReturn(getCellGridMock(b));
				Mockito.when(state.getTopography().getAttributesPedestrian().getRadius()).thenReturn(0.195);

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 1, 2), b.getDistByPedId(1,2));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 1, 3), b.getDistByPedId(1,3));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 1, 4), b.getDistByPedId(1,4));

				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 2, 1), b.getDistByPedId(2,1));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 2, 3), b.getDistByPedId(2,3));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 2, 4), b.getDistByPedId(2,4));

				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 3, 1), b.getDistByPedId(3,1));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 3, 2), b.getDistByPedId(3,2));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 3, 4), b.getDistByPedId(3,4));

				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 4, 1), b.getDistByPedId(4,1));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 4, 2), b.getDistByPedId(4,2));
				addToExpectedOutput(new TimestepPedestrianIdOverlap(step, 4, 3), b.getDistByPedId(4,3));
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
					StringJoiner js = new StringJoiner(getDelimiter());
					js.add(Integer.toString(e.getKey().getTimeStep()))
							.add(Integer.toString(e.getKey().getPedId1()))
							.add(Integer.toString(e.getKey().getPedId2()))
							.add(Double.toString(e.getValue()));
					outputList.add(js.toString());
				});
		return outputList;
	}
}
