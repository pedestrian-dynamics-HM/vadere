package org.vadere.simulator.projects.dataprocessing.processor;

import org.mockito.Mockito;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.models.potential.PotentialFieldModel;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepRowKey;
import org.vadere.simulator.utils.PedestrianListBuilder;
import org.vadere.state.attributes.processor.AttributesFloorFieldProcessor;
import org.vadere.state.scenario.Agent;
import org.vadere.util.data.FloorFieldGridRow;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TargetFloorFieldGridProcessorTestEnv extends ProcessorTestEnv<TimestepRowKey, FloorFieldGridRow> {

	PedestrianListBuilder b = new PedestrianListBuilder();

	TargetFloorFieldGridProcessorTestEnv() {
		super(TargetFloorFieldGridProcessor.class, TimestepRowKey.class);
	}

	@Override
	void initializeDependencies() {
		AttributesFloorFieldProcessor attr =
				(AttributesFloorFieldProcessor) testedProcessor.getAttributes();
		attr.setResolution(1.0);
		attr.setTargetId(1);
	}

	@Override
	public void loadDefaultSimulationStateMocks() {
		MainModel model = mock(OptimalStepsModel.class, Mockito.RETURNS_DEEP_STUBS);
		IPotentialFieldTarget pft = mock(IPotentialFieldTarget.class);
		when(pft.getPotential(any(VPoint.class), any(Agent.class)))
				.thenReturn(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
		addSimState(new SimulationStateMock(1) {
			@Override
			public void mockIt() {
				when(state.getMainModel()).thenReturn(Optional.of(model));
				when(((PotentialFieldModel) model).getPotentialFieldTarget()).thenReturn(pft);
				when(state.getTopography().getBounds()).thenReturn(new Rectangle2D.Double(0.0, 0.0, 3.0, 2.0));
				when(pft.needsUpdate()).thenReturn(false);
				b.clear().add(1);
				when(state.getTopography().getPedestrianDynamicElements().getElements()).thenReturn(b.getList());

				int step = state.getStep();
				addToExpectedOutput(new TimestepRowKey(step, 0), createGrid(1.0, 2.0, 3.0));
				addToExpectedOutput(new TimestepRowKey(step, 1), createGrid(4.0, 5.0, 6.0));
			}
		});
	}

	private FloorFieldGridRow createGrid(Double... val) {
		FloorFieldGridRow out = new FloorFieldGridRow(val.length);
		for (int i = 0; i < val.length; i++) {
			out.setValue(i, val[i]);
		}
		return out;
	}

	@Override
	List<String> getExpectedOutputAsList() {
		List<String> outputList = new ArrayList<>();
		expectedOutput.entrySet()
				.stream()
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.forEach(e -> {
					StringJoiner sj = new StringJoiner(getDelimiter());
					sj.add(Integer.toString(e.getKey().getTimeStep()))
							.add(Integer.toString(e.getKey().getRow()));
					String[] value = e.getValue().toStrings();
					for (String aValue : value) {
						sj.add(aValue);
					}
					outputList.add(sj.toString());
				});
		return outputList;
	}
}
