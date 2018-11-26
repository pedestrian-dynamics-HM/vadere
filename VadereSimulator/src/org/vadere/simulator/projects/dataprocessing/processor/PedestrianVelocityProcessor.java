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
 * This processor computes the velocity based on pedestrian positions and the simulation time.
 * Let p be the position of a pedestrian at the current time step s. And let q be the position
 * of the same pedestrian at the time step s minus <tt>backSteps</tt> then the velocity
 * of this pedestrian at the time of time step s is defined by:
 * the length of the vector q-p divided by the duration between s and (s minus <tt>backSteps</tt>).
 *
 * @author Benedikt Zoennchen
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
		pedPosProc.update(state);

		Integer timeStep = state.getStep();
		state.getTopography().getElements(Pedestrian.class)
				.stream()
				.map(ped -> ped.getId())
				.forEach(pedId -> putValue(new TimestepPedestrianIdKey(timeStep, pedId), getVelocity(timeStep, state.getSimTimeInSec(), pedId)));

		if (lastSimTimes.size() >= backSteps) {
			lastSimTimes.removeLast();
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
		double duration = (currentSimTime - lastSimTimes.getFirst());

		double velocity = posNow.subtract(posBefore).scalarMultiply(1 / duration).distanceToOrigin();

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
