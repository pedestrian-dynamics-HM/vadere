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
 * Evaluates the length of a parade, based on the PedestrianPotentialProcessor.
 * The length of the parade is given in travel time by calculating the difference in the potential.
 * The difference in potential is calculated between the average of the first numberOfAgentsAveraged agents and the
 * average of the last numberOfAgentsAveraged agents.
 * The parade length is only evaluated for the timesteps in which all agents are spawned.
 * That means, it will not work with continuous spawning.
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
		double timeAllSpawned = Collections.max(pedestrianStartTimeProcessor.getValues());
		int timestepAllSpawned = (int) Math.round(timeAllSpawned / simTimeStepLength);

		// find last timestep (first agent reaches its target) [s]
		double timeFirstLeaves = Collections.min(pedestrianEvacuationTimeProcessor.getValues());
		int timestepFirstLeaves = (int) Math.round(timeFirstLeaves / simTimeStepLength);

		// find all timesteps between timeAllSpawned and timeFirstLeaves
		Set<TimestepPedestrianIdKey> timestepKeys = pedestrianPotentialProcessor.getKeys().stream().
				filter(key -> (key.getTimestep() >= timestepAllSpawned && key.getTimestep() < timestepFirstLeaves)).collect(Collectors.toSet());

		Set<Integer> timesteps = new HashSet<>();
		timestepKeys.stream().forEach(key -> timesteps.add(key.getTimestep()));

		// for each time step between timestepAllSpawned and timestepFirstLeaves, calculate the parade length
		for (Integer timestep : timesteps) {
			// get all TimestepPedestrianIdKey in potential for this timestep
			List<TimestepPedestrianIdKey> timestepKeyi = timestepKeys.stream().filter(key -> key.getTimestep().equals(timestep)).collect(Collectors.toList());

			calculateParadeLengthPerTimestep(timestepKeyi, timestep.intValue());
		}

	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesParadeLengthProcessor());
		}

		return super.getAttributes();
	}

	private void calculateParadeLengthPerTimestep(List<TimestepPedestrianIdKey> timestepKeyi, int timestep ) {

		// collect all potential values
		TreeSet<Double> potential = new TreeSet();
		timestepKeyi.stream().forEach(key -> potential.add(pedestrianPotentialProcessor.getValue(key)));

		if (potential.size() >= this.numberOfAgentsAveraged) {
			// todo: consider using median instead of average to avoid impact of agents which far ahead / behind

			// find the  lowest numberOfAgentsAveraged values of the potential and average them
			OptionalDouble minPotential = potential.stream().limit(this.numberOfAgentsAveraged).mapToDouble(p -> p).average();

			// find the  highest numberOfAgentsAveraged values of the potential and average them
			List<Double> shortlistPotential = new ArrayList(potential);
			OptionalDouble maxPotential = shortlistPotential.subList(potential.size() - this.numberOfAgentsAveraged, potential.size()).stream().mapToDouble(p -> p).average();

			if (minPotential.isPresent() && maxPotential.isPresent()) {
				// paradeLength is the difference between the highest and lowest potential values
				// todo: transform to meters with speed, e.g. freeflowspeedmean
				double paradeLength = maxPotential.getAsDouble() - minPotential.getAsDouble();
				putValue(new TimestepKey(timestep), paradeLength);
			}
		} else {
			logger.warn("Make sure that numberOfAgentsAveraged is smaller than your spawnNumber!");
		}
	}
}
