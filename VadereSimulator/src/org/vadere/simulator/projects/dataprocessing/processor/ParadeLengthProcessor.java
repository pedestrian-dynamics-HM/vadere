package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.attributes.processor.AttributesParadeLengthProcessor;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Marion Goedel
 * Evaluates the length of a parade, based on the PedestrianPotentialProcessor
 */

@DataProcessorClass()
public class ParadeLengthProcessor extends DataProcessor<TimestepKey, Double> {
	private PedestrianPotentialProcessor pedestrianPotentialProcessor;
	private PedestrianEvacuationTimeProcessor pedestrianEvacuationTimeProcessor;
	private PedestrianStartTimeProcessor pedestrianStartTimeProcessor;
	private int numberOfAgentsAveraged;

	private static Logger logger = Logger.getLogger(ParadeLengthProcessor.class);


	public ParadeLengthProcessor() {
		super("paradeLength");
		setAttributes(new AttributesParadeLengthProcessor());
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		//ensure that all required DataProcessors are updated.
		this.pedestrianPotentialProcessor.update(state);
		this.pedestrianStartTimeProcessor.update(state);
		this.pedestrianEvacuationTimeProcessor.update(state);
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesParadeLengthProcessor att = (AttributesParadeLengthProcessor) this.getAttributes();
		this.pedestrianPotentialProcessor = (PedestrianPotentialProcessor) manager.getProcessor(att.getPedestrianPotentialProcessorId());
		this.pedestrianStartTimeProcessor = (PedestrianStartTimeProcessor) manager.getProcessor(att.getPedestrianStartTimeProcessor());
		this.pedestrianEvacuationTimeProcessor = (PedestrianEvacuationTimeProcessor) manager.getProcessor(att.getPedestrianEvacuationTimeProcessor());
		this.numberOfAgentsAveraged = att.getNumberOfAgentsAveraged();

	}

	@Override
	public void postLoop(final SimulationState state) {
		pedestrianPotentialProcessor.postLoop(state);
		pedestrianStartTimeProcessor.postLoop(state);
		pedestrianEvacuationTimeProcessor.postLoop(state);


		double simTimeStepLength = state.getScenarioStore().getAttributesSimulation().getSimTimeStepLength();

		// find first timestep (last agent is spawned) [s]
		double time_all_spawned = Collections.max(pedestrianStartTimeProcessor.getValues());
		int time_step_all_spawned = (int) Math.round(time_all_spawned / simTimeStepLength);

		// find last timestep (first agent reaches its target) [s]
		double time_first_leaves = Collections.min(pedestrianEvacuationTimeProcessor.getValues());
		int time_step_first_leaves = (int) Math.round(time_first_leaves / simTimeStepLength);

		// find all Timesteps between time_all_spawned and time_first_leaves
		Set<Integer> time_steps = new HashSet<>();

		Set<TimestepPedestrianIdKey> time_step_keys = pedestrianPotentialProcessor.getKeys().stream().
				filter(key -> (key.getTimestep() >= time_step_all_spawned && key.getTimestep() < time_step_first_leaves)).collect(Collectors.toSet());

		time_step_keys.stream().forEach(key -> time_steps.add(key.getTimestep()));

		for (Integer time_step : time_steps) {
			List<TimestepPedestrianIdKey> time_step_keys_i = time_step_keys.stream().filter(key -> key.getTimestep().equals(time_step)).collect(Collectors.toList());

			TreeSet<Double> potential = new TreeSet();
			time_step_keys_i.stream().forEach(key -> potential.add(pedestrianPotentialProcessor.getValue(key)));

			if(potential.size() >= this.numberOfAgentsAveraged) {
				OptionalDouble min_potential = potential.stream().limit(this.numberOfAgentsAveraged).mapToDouble(p -> p).average();
				List<Double> f = new ArrayList(potential);
				OptionalDouble max_potential = f.subList(potential.size() - this.numberOfAgentsAveraged, potential.size()).stream().mapToDouble(p -> p).average();

				if (min_potential.isPresent() && max_potential.isPresent()) {
					double paradeLength = max_potential.getAsDouble() - min_potential.getAsDouble();
					putValue(new TimestepKey(time_step), paradeLength);
				}
			}else{
				logger.warn("Make sure that numberOfAgentsAveraged is smaller than your spawnNumber!");
			}


		}
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesParadeLengthProcessor());
		}

		return super.getAttributes();
	}
}
