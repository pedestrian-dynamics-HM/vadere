package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.processor.AttributesNumberOverlapsProcessor;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class NumberOverlapsProcessorTestEnv extends ProcessorTestEnv<NoDataKey, Long> {

	private PedestrianListBuilder b = new PedestrianListBuilder();

	NumberOverlapsProcessorTestEnv() {
		this(1);
	}

	private NumberOverlapsProcessorTestEnv(int nextProcessorId) {
		super(NumberOverlapsProcessor.class, NoDataKey.class, nextProcessorId);
	}

	@Override
	void initializeDependencies() {
		int pedestrianOverlapProcessorId = addDependentProcessor(PedestrianOverlapProcessorTestEnv::new);
		//add ProcessorId of required Processors to current Processor under test
		AttributesNumberOverlapsProcessor attr = (AttributesNumberOverlapsProcessor) testedProcessor.getAttributes();
		attr.setPedestrianOverlapProcessorId(pedestrianOverlapProcessorId);
	}

	private LinkedCellsGrid<DynamicElement> getCellGridMock(PedestrianListBuilder b) {
		LinkedCellsGrid<DynamicElement> cellsGrid = new LinkedCellsGrid<>(0.0, 0.0, 10.0, 10.0, 1);
		b.getDynamicElementList().forEach(cellsGrid::addObject);
		return cellsGrid;
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
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

				addToExpectedOutput(NoDataKey.key(), 2L);
			}
		});
	}

	void noOverlapsMock() {
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

				addToExpectedOutput(NoDataKey.key(), 0L);
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
					sj.add(Long.toString(e.getValue()));
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
