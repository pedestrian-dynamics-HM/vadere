package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianVelocityByTrajectoryProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;

import java.util.LinkedList;

/**
 * This processor computes the velocity based on pedestrian trajectories i.e. a list of foot steps.
 * This might be more accurate than using {@link PedestrianVelocityProcessor} which uses only positions.
 * For example in the optimal steps model a pedestrian might move multiple times during one time step.
 * Let t = (p1,p2), (p2,p3), and so on, (pn,pn+1) be the trajectory of a pedestrian at the current time step s.
 * Then the processor cuts the trajectory based on how much the processor should look into the past which is
 * controlled by <tt>backSteps</tt>. By cutting the trajectory is linear interpolated i.e. the cut don't have to be
 * at the end or the beginning of a foot step but can be in between. The velocity is equal to the sum of all
 * length of foot steps divided by the duration.
 *
 * @author Benedikt Zoennchen
 *
 */
@DataProcessorClass()
public class PedestrianVelocityByTrajectoryProcessor extends APedestrianVelocityProcessor {
	private PedestrianTrajectoryProcessor pedestrianTrajectoryProcessor;
	private int backSteps;

	private LinkedList<Double> lastSimTimes;

	public PedestrianVelocityByTrajectoryProcessor() {
		super();
		setAttributes(new AttributesPedestrianVelocityByTrajectoryProcessor());
		this.lastSimTimes = new LinkedList<>();
		this.lastSimTimes.add(0.0);
	}

	@Override
	public void doUpdate(final SimulationState state) {
		pedestrianTrajectoryProcessor.update(state);

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
		AttributesPedestrianVelocityByTrajectoryProcessor attVelProc = (AttributesPedestrianVelocityByTrajectoryProcessor) getAttributes();
		this.pedestrianTrajectoryProcessor = (PedestrianTrajectoryProcessor) manager.getProcessor(attVelProc.getPedestrianTrajectoryProcessorId());
		this.backSteps = attVelProc.getBackSteps();
		this.lastSimTimes = new LinkedList<>();
		this.lastSimTimes.add(0.0);
	}

	private double getVelocity(int timeStep, double currentSimTime, int pedId) {

		int pastStep = timeStep - backSteps;
		double velocity = 0.0;
		if(pastStep >= 1) {

			double startTime = lastSimTimes.getFirst();
			double endTime = currentSimTime;
			double duration = (endTime - startTime);
			velocity = pedestrianTrajectoryProcessor.getValue(new PedestrianIdKey(pedId)).cut(startTime, endTime).speed().orElse(0.0);
		}

		return velocity;
	}

	@Override
	public AttributesProcessor getAttributes() {
		if(super.getAttributes() == null) {
			setAttributes(new AttributesPedestrianVelocityByTrajectoryProcessor());
		}

		return super.getAttributes();
	}
}
