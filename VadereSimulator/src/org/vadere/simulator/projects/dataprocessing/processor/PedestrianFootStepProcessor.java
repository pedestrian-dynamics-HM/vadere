package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.util.StateJsonConverter;

import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * During one time step a pedestrian my move multiple times which is saved by {@link Pedestrian#getFootSteps()}, i.e. the list of {@link FootStep}s
 * will be adjusted after each update(simTimeInSec) call such that it contains the foot steps which started at the lastSimTimeInSec!
 *
 * This processor writes out all those {@link FootStep}s using the standard JSON-format, e.g. one foot steps:
 * [{"startTime":26.588661014252686,"endTime":27.123123483931312,"start":{"x":29.4730189272315,"y":24.965262390895376},"end":{"x":29.59817287115996,"y":25.182035380547074}}]
 *
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class PedestrianFootStepProcessor extends DataProcessor<TimestepPedestrianIdKey, LinkedList<FootStep>>{

	private double lastSimTime;

	public PedestrianFootStepProcessor() {
		super("strides");
		lastSimTime = 0.0;
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		Integer timeStep = state.getStep();
		for (Pedestrian pedestrian : state.getTopography().getElements(Pedestrian.class)) {
			LinkedList<FootStep> copy = pedestrian.getFootSteps()
					.stream()
					//.filter(footStep -> footStep.getEndTime() > lastSimTime)
					//.filter(footStep -> footStep.getEndTime() <= state.getSimTimeInSec())
					.collect(Collectors.toCollection(LinkedList::new));

			putValue(new TimestepPedestrianIdKey(timeStep, pedestrian.getId()), copy);
		}
		lastSimTime = state.getSimTimeInSec();
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

	@Override
	public String[] toStrings(TimestepPedestrianIdKey key) {
		LinkedList<FootStep> strides = this.getValue(key);
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