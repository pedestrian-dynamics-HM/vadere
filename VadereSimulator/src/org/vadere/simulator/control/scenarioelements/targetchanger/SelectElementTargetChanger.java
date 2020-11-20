package org.vadere.simulator.control.scenarioelements.targetchanger;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.TargetChangerAlgorithmType;
import org.vadere.state.scenario.Topography;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class SelectElementTargetChanger extends BaseTargetChangerAlgorithm {

	private EnumeratedIntegerDistribution dist;

	public SelectElementTargetChanger(TargetChanger targetChanger, Topography topography) {
		super(targetChanger, topography);
	}

	@Override
	public void throwExceptionOnInvalidInput(TargetChanger targetChanger) {
		int totalTargets = targetChanger.getAttributes().getNextTarget().size();
		int totalProbabilities = targetChanger.getAttributes().getProbabilitiesToChangeTarget().size();

		boolean inputIsValid = (totalProbabilities == 0 && totalTargets > 0) ||
				(totalProbabilities == totalTargets ) ||
				(totalProbabilities == totalTargets+1 );

		if (!inputIsValid) {
			throw new IllegalArgumentException(String.format(
					"The size of \"probabilitiesToChangeTarget\" must be 0 or the same length as nextTarget. %s", TargetChangerAlgorithmType.SELECT_LIST) );
		}
	}

	@Override
	public void init(Random rnd) {
		int nextTargetSize = targetChanger.getAttributes().getNextTarget().size();
		int probabilitySize = targetChanger.getAttributes().getProbabilitiesToChangeTarget().size();
		int arrSize;
		if (probabilitySize == 0){
			arrSize = nextTargetSize;
		} else {
			arrSize = Math.max(nextTargetSize, probabilitySize);
		}
		int[] entity = IntStream.range(0, arrSize).toArray();
		double[] probability = new double[arrSize]; //
		if (probabilitySize == 0){
			// no probability given thus uniform distribution
			Arrays.fill(probability, 1.0 /nextTargetSize);
		} else {
			// probability same size as nextTargetSize (or one bigger for no change!)
			double sum = getTargetChanger().getAttributes().getProbabilitiesToChangeTarget().stream().reduce(0.0, Double::sum);
			double norm = 1.0;
			if (sum > 1.0)
				norm = sum;

			for(int i=0; i < probability.length; i++){
				double val = targetChanger.getAttributes().getProbabilitiesToChangeTarget().get(i);
				probability[i] = val/norm;
			}
		}
		JDKRandomGenerator randomGenerator = new JDKRandomGenerator();
		randomGenerator.setSeed(rnd.nextInt());
		dist = new EnumeratedIntegerDistribution(randomGenerator, entity, probability);
	}

	@Override
	public boolean setAgentTargetList(Agent agent) {
		int index = dist.sample();

		if (index == targetChanger.getAttributes().getNextTarget().size())
			return false; // do not change target.

		agent.setSingleTarget(targetChanger.getAttributes().getNextTarget().get(index), false);
		agent.setNextTargetListIndex(0);
		agent.setIsCurrentTargetAnAgent(false);
		return true;

	}
}
