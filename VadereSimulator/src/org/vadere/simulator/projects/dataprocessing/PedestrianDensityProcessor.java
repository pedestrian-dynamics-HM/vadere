package org.vadere.simulator.projects.dataprocessing;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.Pedestrian;

public abstract class PedestrianDensityProcessor extends Processor<TimestepPedestrianIdDataKey, Double> {
	private PedestrianPositionProcessor pedPosProc;
	private IPointDensityAlgorithm densAlg;

	protected void setAlgorithm(IPointDensityAlgorithm densAlg) {
		this.densAlg = densAlg;
		this.setHeader(this.densAlg.getName().toLowerCase() + "-density");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		this.pedPosProc.update(state);

		state.getTopography().getElements(Pedestrian.class).stream()
				.forEach(ped -> this.addValue(new TimestepPedestrianIdDataKey(state.getStep(), ped.getId()),
						this.densAlg.getDensity(ped.getPosition(), state)));
	}

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		AttributesPedestrianDensityProcessor attDensProc = (AttributesPedestrianDensityProcessor) attributes;

		this.pedPosProc =
				(PedestrianPositionProcessor) manager.getProcessor(attDensProc.getPedestrianPositionProcessorId());
	}
}
