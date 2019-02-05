package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.state.util.StateJsonConverter;

import java.util.LinkedList;

/**
 * <p>During one time step a pedestrian my move multiple times which is saved by
 * {@link Pedestrian#getFootSteps()}, i.e. the {@link VTrajectory} will be adjusted
 * after each update(simTimeInSec) call such that it contains the foot steps which
 * started at the lastSimTimeInSec!</p>
 *
 * <p>This processor writes out all those {@link FootStep}s using the standard JSON-format, e.g. one foot steps:
 * [{"startTime":26.588661014252686,"endTime":27.123123483931312,"start":{"x":29.4730189272315,"y":24.965262390895376},"end":{"x":29.59817287115996,"y":25.182035380547074}}]
 * </p>
 *
 * <p>This is especially useful if one
 * uses the {@link org.vadere.simulator.models.osm.OptimalStepsModel} or any other
 * {@link org.vadere.simulator.models.MainModel} for which pedestrians do multiple steps during
 * a simulation time step.</p>
 *
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class PedestrianFootStepProcessor extends DataProcessor<TimestepPedestrianIdKey, VTrajectory>{

	public PedestrianFootStepProcessor() {
		super("strides");
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		Integer timeStep = state.getStep();
		for (Pedestrian pedestrian : state.getTopography().getElements(Pedestrian.class)) {
			VTrajectory copy = pedestrian.getFootSteps().clone();
			putValue(new TimestepPedestrianIdKey(timeStep, pedestrian.getId()), copy);
		}
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

	@Override
	public String[] toStrings(TimestepPedestrianIdKey key) {
		LinkedList<FootStep> strides = this.getValue(key).getFootSteps();
		StringBuilder builder = new StringBuilder();

		if(strides == null) {
			return new String[]{"{}"};
		}
		else {
			builder.append("[");
			String stridesString = StateJsonConverter.serialidzeObject(strides);
			builder.append("]");

			return new String[]{stridesString};
		}
	}
}