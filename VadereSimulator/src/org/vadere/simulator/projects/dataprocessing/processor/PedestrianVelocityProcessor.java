package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianVelocityProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.LinkedList;

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
public class PedestrianVelocityProcessor extends APedestrianVelocityProcessor {
	private PedestrianPositionProcessor pedestrianPositionProcessor;
	private int backSteps;

	private LinkedList<Double> lastSimTimes;

	public PedestrianVelocityProcessor() {
		super();
		setAttributes(new AttributesPedestrianVelocityProcessor());
		this.lastSimTimes = new LinkedList<>();
		this.lastSimTimes.add(0.0);
	}

	@Override
	public void doUpdate(final SimulationState state) {
		pedestrianPositionProcessor.update(state);

		Integer timeStep = state.getStep();
		state.getTopography().getElements(Pedestrian.class)
				.stream()
				.map(ped -> ped.getId())
				.forEach(pedId -> putValue(new TimestepPedestrianIdKey(timeStep, pedId), getVelocity(timeStep, state.getSimTimeInSec(), pedId)));

		if (lastSimTimes.size() >= backSteps) {
			lastSimTimes.removeFirst();
		}

		lastSimTimes.addLast(state.getSimTimeInSec());
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesPedestrianVelocityProcessor attVelProc = (AttributesPedestrianVelocityProcessor) getAttributes();
		this.pedestrianPositionProcessor = (PedestrianPositionProcessor) manager.getProcessor(attVelProc.getPedestrianPositionProcessorId());
		this.backSteps = attVelProc.getBackSteps();
		this.lastSimTimes = new LinkedList<>();
		this.lastSimTimes.add(0.0);
	}

	private double getVelocity(int timeStep, double currentSimTime, int pedId) {

		int pastStep = Math.max(1, timeStep - backSteps);
		double velocity = 0.0;
		if(timeStep > 1) {

			VPoint pastPosition = pedestrianPositionProcessor.getValue(new TimestepPedestrianIdKey(pastStep, pedId));
			VPoint position = pedestrianPositionProcessor.getValue(new TimestepPedestrianIdKey(timeStep, pedId));

			if(pastPosition != null) {
				double startTime = lastSimTimes.getFirst();
				double endTime = currentSimTime;
				double duration = (endTime - startTime);

				velocity = position.subtract(pastPosition).scalarMultiply(1.0 / duration).distanceToOrigin();
			}
		}

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
