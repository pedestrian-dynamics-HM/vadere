package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianVelocityProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.LinkedList;
import java.util.stream.Stream;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass()
public class PedestrianVelocityProcessor extends DataProcessor<TimestepPedestrianIdKey, Double> {
	private PedestrianPositionProcessor pedPosProc;
	private int backSteps;

	private LinkedList<Double> lastSimTimes;

	public PedestrianVelocityProcessor() {
		super("velocity");
		setAttributes(new AttributesPedestrianVelocityProcessor());
		this.lastSimTimes = new LinkedList<>();
		this.lastSimTimes.add(0.0);
	}

	@Override
	public void doUpdate(final SimulationState state) {
		this.pedPosProc.update(state);

		Integer timeStep = state.getStep();
		Stream<Integer> pedIds = state.getTopography().getElements(Pedestrian.class).stream().map(ped -> ped.getId());

		pedIds.forEach(pedId -> this.putValue(new TimestepPedestrianIdKey(timeStep, pedId),
				this.getVelocity(timeStep, state.getSimTimeInSec(), pedId)));

		if (this.lastSimTimes.size() >= this.backSteps)
			this.lastSimTimes.removeLast();
		this.lastSimTimes.addFirst(state.getSimTimeInSec());
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesPedestrianVelocityProcessor attVelProc = (AttributesPedestrianVelocityProcessor) getAttributes();

		this.pedPosProc =
				(PedestrianPositionProcessor) manager.getProcessor(attVelProc.getPedestrianPositionProcessorId());
		this.backSteps = attVelProc.getBackSteps();
		this.lastSimTimes = new LinkedList<>();
		this.lastSimTimes.add(0.0);
	}

	private Double getVelocity(int timeStep, double currentSimTime, int pedId) {
		TimestepPedestrianIdKey keyBefore = new TimestepPedestrianIdKey(timeStep - this.backSteps > 0 ? timeStep - this.backSteps : 1, pedId);

		if (timeStep <= 1 || !this.pedPosProc.hasValue(keyBefore))
			return 0.0; // For performance

		VPoint posBefore = this.pedPosProc.getValue(keyBefore);
		VPoint posNow = this.pedPosProc.getValue(new TimestepPedestrianIdKey(timeStep, pedId));

		return posNow.subtract(posBefore).scalarMultiply(1 / (currentSimTime - this.lastSimTimes.getFirst()))
				.distanceToOrigin();
	}

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesPedestrianVelocityProcessor());
        }

        return super.getAttributes();
    }
}
