package org.vadere.simulator.projects.dataprocessing.processors;

import java.util.LinkedList;
import java.util.stream.Stream;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakeys.TimestepPedestrianIdDataKey;
import org.vadere.state.attributes.processors.AttributesProcessor;
import org.vadere.state.attributes.processors.AttributesVelocityProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

public class PedestrianVelocityProcessor extends Processor<TimestepPedestrianIdDataKey, Double> {
	private PedestrianPositionProcessor pedPosProc;
	private int backSteps;

	private LinkedList<Double> lastSimTimes;

	public PedestrianVelocityProcessor() {
		super("v");

		this.lastSimTimes = new LinkedList<>();
		this.lastSimTimes.add(0.0);
	}

	@Override
	public void doUpdate(final SimulationState state) {
		this.pedPosProc.update(state);

		Integer timeStep = state.getStep();
		Stream<Integer> pedIds = state.getTopography().getElements(Pedestrian.class).stream().map(ped -> ped.getId());

		pedIds.forEach(pedId -> this.addValue(new TimestepPedestrianIdDataKey(timeStep, pedId),
				this.getVelocity(timeStep, state.getSimTimeInSec(), pedId)));

		if (this.lastSimTimes.size() >= this.backSteps)
			this.lastSimTimes.removeLast();
		this.lastSimTimes.addFirst(state.getSimTimeInSec());
	}

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		AttributesVelocityProcessor attVelProc = (AttributesVelocityProcessor) attributes;

		this.pedPosProc =
				(PedestrianPositionProcessor) manager.getProcessor(attVelProc.getPedestrianPositionProcessorId());
		this.backSteps = attVelProc.getBackSteps();
	}

	private Double getVelocity(int timeStep, double currentSimTime, int pedId) {
		TimestepPedestrianIdDataKey keyBefore = new TimestepPedestrianIdDataKey(timeStep - this.backSteps > 0 ? timeStep - this.backSteps : 1, pedId);

		if (timeStep <= 1 || !this.pedPosProc.hasValue(keyBefore))
			return 0.0; // For performance

		VPoint posBefore = this.pedPosProc.getValue(keyBefore);
		VPoint posNow = this.pedPosProc.getValue(new TimestepPedestrianIdDataKey(timeStep, pedId));

		return posNow.subtract(posBefore).scalarMultiply(1 / (currentSimTime - this.lastSimTimes.getFirst()))
				.distanceToOrigin();
	}
}
