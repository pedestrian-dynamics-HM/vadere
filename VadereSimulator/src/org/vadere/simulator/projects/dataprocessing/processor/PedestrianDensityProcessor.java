package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdDataKey;
import org.vadere.state.attributes.processor.AttributesPedestrianDensityProcessor;
import org.vadere.state.scenario.Pedestrian;

public abstract class PedestrianDensityProcessor extends DataProcessor<TimestepPedestrianIdDataKey, Double> {
	private PedestrianPositionProcessor pedPosProc;
	private IPointDensityAlgorithm densAlg;

	protected void setAlgorithm(IPointDensityAlgorithm densAlg) {
		this.densAlg = densAlg;
		this.setHeader(this.densAlg.getName() + "Density");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		this.pedPosProc.update(state);

		state.getTopography().getElements(Pedestrian.class).stream()
				.forEach(ped -> this.addValue(new TimestepPedestrianIdDataKey(state.getStep(), ped.getId()),
						this.densAlg.getDensity(ped.getPosition(), state)));
	}

	@Override
	public void init(final ProcessorManager manager) {
		AttributesPedestrianDensityProcessor attDensProc = (AttributesPedestrianDensityProcessor) this.getAttributes();

		this.pedPosProc =
				(PedestrianPositionProcessor) manager.getProcessor(attDensProc.getPedestrianPositionProcessorId());
	}
}
