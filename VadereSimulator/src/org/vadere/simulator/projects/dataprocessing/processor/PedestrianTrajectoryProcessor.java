package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.state.util.StateJsonConverter;

import java.util.Collection;

/**
 * This processor writes out the {@link VTrajectory} for each {@link Pedestrian} of the simulation.
 * One such {@link VTrajectory} will be written to one line. Note that {@link VTrajectory} gives
 * more details like the exact start and end time of a step which is especially useful if one
 * uses the {@link org.vadere.simulator.models.osm.OptimalStepsModel} or any other
 * {@link org.vadere.simulator.models.MainModel} for which pedestrians do multiple steps during
 * a simulation time step.
 *
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class PedestrianTrajectoryProcessor extends DataProcessor<PedestrianIdKey, VTrajectory> {

	public PedestrianTrajectoryProcessor() {
		super("trajectory");
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

		for(Pedestrian ped : peds) {
			PedestrianIdKey key = new PedestrianIdKey(ped.getId());
			getValue(key).concat(ped.getTrajectory());
		}
	}

	@Override
	public VTrajectory getValue(PedestrianIdKey key) {
		if(super.getValue(key) == null) {
			super.putValue(key, new VTrajectory());
		}
		return super.getValue(key);
	}

	@Override
	public String[] toStrings(PedestrianIdKey key) {
		return new String[]{"[" + StateJsonConverter.serializeObject(getValue(key)) + "]"};
	}
}
