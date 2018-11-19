package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
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
	private int lastTimeStep;

	private LinkedList<Double> lastSimTimes;

	public PedestrianVelocityProcessor() {
		super("velocity");
		setAttributes(new AttributesPedestrianVelocityProcessor());
		this.lastSimTimes = new LinkedList<>();
		this.lastSimTimes.add(0.0);
	}

	@Override
	public void doUpdate(final SimulationState state) {
		if(state.getStep() > lastTimeStep) {
			pedPosProc.update(state);

			Integer timeStep = state.getStep();
			Stream<Integer> pedIds = state.getTopography().getElements(Pedestrian.class).stream().map(ped -> ped.getId());

			pedIds.forEach(pedId -> putValue(new TimestepPedestrianIdKey(timeStep, pedId),
					getVelocity(timeStep, state.getSimTimeInSec(), pedId)));

			if (lastSimTimes.size() >= backSteps) {
				lastSimTimes.removeLast();
			}

			lastSimTimes.addFirst(state.getSimTimeInSec());
			lastTimeStep = state.getStep();
		}
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

	private double getVelocity(int timeStep, double currentSimTime, int pedId) {
		TimestepPedestrianIdKey keyBefore = new TimestepPedestrianIdKey(timeStep - backSteps > 0 ? timeStep - backSteps : 1, pedId);

		if (timeStep <= 1 || !pedPosProc.hasValue(keyBefore))
			return 0.0; // For performance

		VPoint posBefore = pedPosProc.getValue(keyBefore);
		VPoint posNow = pedPosProc.getValue(new TimestepPedestrianIdKey(timeStep, pedId));

		double velocity = posNow.subtract(posBefore).scalarMultiply(1 / (currentSimTime - lastSimTimes.getFirst())).distanceToOrigin();

		return velocity;
	}

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesPedestrianVelocityProcessor());
        }

        return super.getAttributes();
    }
}
