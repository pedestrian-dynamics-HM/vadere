package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianVelocityProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.VTrajectory;
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
	private PedestrianTrajectoryProcessor pedTrajProc;
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
		pedTrajProc.update(state);

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
		this.pedTrajProc = (PedestrianTrajectoryProcessor) manager.getProcessor(attVelProc.getPedestrianTrajectoryProcessorId());
		this.backSteps = attVelProc.getBackSteps();
		this.lastSimTimes = new LinkedList<>();
		this.lastSimTimes.add(0.0);
	}

	private double getVelocity(int timeStep, double currentSimTime, int pedId) {

		int pastStep = timeStep - backSteps;
		double velocity = 0.0;
		if(pastStep >= 0) {
			TimestepPedestrianIdKey keyBefore = new TimestepPedestrianIdKey(pastStep, pedId);

			if (timeStep <= 1)
				return 0.0; // For performance

			VTrajectory trajectory = pedTrajProc.getValue(new PedestrianIdKey(pedId));
			double startTime = lastSimTimes.getFirst();
			double endTime = currentSimTime;
			double duration = (endTime - startTime);

			velocity = trajectory.cut(startTime, endTime).speed().orElse(0.0);

			return velocity;
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
