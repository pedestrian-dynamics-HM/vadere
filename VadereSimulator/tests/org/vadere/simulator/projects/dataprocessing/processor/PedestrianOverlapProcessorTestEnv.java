package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.datakey.OverlapData;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdOverlapKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VPoint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PedestrianOverlapProcessorTestEnv extends ProcessorTestEnv<TimestepPedestrianIdOverlapKey, OverlapData> {

	private PedestrianListBuilder b = new PedestrianListBuilder();

	PedestrianOverlapProcessorTestEnv() {
		this(1);
	}

	PedestrianOverlapProcessorTestEnv(int processorId) {
		super(PedestrianOverlapProcessor.class, TimestepPedestrianIdOverlapKey.class, processorId);
	}

	protected LinkedCellsGrid<DynamicElement> getCellGridMock(PedestrianListBuilder b) {
		LinkedCellsGrid<DynamicElement> cellsGrid = new LinkedCellsGrid<>(0.0, 0.0, 10.0, 10.0, 1);
		b.getDynamicElementList().forEach(cellsGrid::addObject);
		return cellsGrid;
	}

	@Override
	public void loadDefaultSimulationStateMocks() {

		AttributesAgent attributesAgent = new AttributesAgent();
		double minDist = new AttributesAgent().getRadius() * 2;

		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				b.clear().add(1, new VPoint(1.0, 1.0));
				b.add(2, new VPoint(1.5, 1.5));
				b.add(3, new VPoint(1.5, 1.0));
				b.add(4, new VPoint(1.0, 1.5));
				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getTopography().getSpatialMap(DynamicElement.class)).thenReturn(getCellGridMock(b));
				Mockito.when(state.getTopography().getAttributesPedestrian().getRadius()).thenReturn(attributesAgent.getRadius());

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
				Mockito.when(state.getTopography().getAttributesPedestrian().getRadius()).thenReturn(attributesAgent.getRadius());

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 1, 5), b.overlapData(1, 5, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 5, 1), b.overlapData(5, 1, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 1, 3), b.overlapData(1, 3, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 3, 1), b.overlapData(3, 1, minDist));
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
				Mockito.when(state.getTopography().getAttributesPedestrian().getRadius()).thenReturn(attributesAgent.getRadius());

				int step = state.getStep();
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 1, 2), b.overlapData(1, 2, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 1, 3), b.overlapData(1, 3, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 1, 4), b.overlapData(1, 4, minDist));

				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 2, 1), b.overlapData(2, 1, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 2, 3), b.overlapData(2, 3, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 2, 4), b.overlapData(2, 4, minDist));

				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 3, 1), b.overlapData(3, 1, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 3, 2), b.overlapData(3, 2, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 3, 4), b.overlapData(3, 4, minDist));

				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 4, 1), b.overlapData(4, 1, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 4, 2), b.overlapData(4, 2, minDist));
				addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 4, 3), b.overlapData(4, 3, minDist));
			}
		});
	}

	void verySmallOverlapping() {
		AttributesAgent a = new AttributesAgent();
		double distAtAxis = a.getRadius() * 2 - 0.001; // this should count as overlap
		double vertDistAt45deg = 2 * Math.sqrt(0.5) * a.getRadius() - 0.001;
		addMockStates(a.getRadius(), distAtAxis, new VPoint(vertDistAt45deg, vertDistAt45deg));
		double minDist = a.getRadius() * 2;
		int step = 1;
		addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 1, 2), b.overlapData(1, 2, minDist));
		addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 2, 1), b.overlapData(2, 1, minDist));
		addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 3, 4), b.overlapData(3, 4, minDist));
		addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 4, 3), b.overlapData(4, 3, minDist));
		addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 5, 6), b.overlapData(5, 6, minDist));
		addToExpectedOutput(new TimestepPedestrianIdOverlapKey(step, 6, 5), b.overlapData(6, 5, minDist));

	}

	void verySmallNotOverlapping() {
		AttributesAgent a = new AttributesAgent();
		double distAtAxis = a.getRadius() * 2 + 0.001; // this should not count as overlap
		double vertDistAt45deg = Math.sqrt(2) * a.getRadius() + 0.001;
		addMockStates(a.getRadius(), distAtAxis, new VPoint(vertDistAt45deg, vertDistAt45deg));
	}

	void touching() {
		AttributesAgent a = new AttributesAgent();
		double distAtAxis = a.getRadius() * 2; // this should count as overlap
		double vertDistAt45deg = round(Math.sqrt(2) * a.getRadius(), 16);
		addMockStates(a.getRadius(), distAtAxis, new VPoint(vertDistAt45deg, vertDistAt45deg));
	}

	private static double round(double val, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bigDecimal = new BigDecimal(val);
		bigDecimal = bigDecimal.setScale(places, RoundingMode.HALF_UP);
		return bigDecimal.doubleValue();
	}


	private void addMockStates(double radius, double distAtAxis, VPoint vertDistAt45deg) {
		clearStates();
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				b.clear();
				b.add(1, new VPoint(1.0, 1.0));
				b.add(2, new VPoint(1.0, 1.0 + distAtAxis));

				b.add(3, new VPoint(3.0, 3.0));
				b.add(4, new VPoint(3.0 + distAtAxis, 3.0));

				b.add(5, new VPoint(6.0, 6.0));
				b.add(6, new VPoint(6.0, 6.0).add(vertDistAt45deg));

				Mockito.when(state.getTopography().getElements(Pedestrian.class)).thenReturn(b.getList());
				Mockito.when(state.getTopography().getSpatialMap(DynamicElement.class)).thenReturn(getCellGridMock(b));
				Mockito.when(state.getTopography().getAttributesPedestrian().getRadius()).thenReturn(radius);

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
							.add(e.getValue().toStrings()[0])
							.add(e.getValue().toStrings()[1]);
					outputList.add(js.toString());
				});
		return outputList;
	}
}
