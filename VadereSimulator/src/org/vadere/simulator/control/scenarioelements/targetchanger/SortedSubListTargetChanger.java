package org.vadere.simulator.control.scenarioelements.targetchanger;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.TargetChangerAlgorithmType;
import org.vadere.state.scenario.Topography;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class SortedSubListTargetChanger extends BaseTargetChangerAlgorithm {


	private ArrayList<BinomialDistribution> binomialDistributions;

	public SortedSubListTargetChanger(TargetChanger targetChanger, Topography topography) {
		super(targetChanger, topography);
		binomialDistributions = new ArrayList<>();
	}

	@Override
	public void throwExceptionOnInvalidInput(TargetChanger targetChanger) {
		int totalTargets = targetChanger.getAttributes().getNextTarget().size();
		int totalProbabilities = targetChanger.getAttributes().getProbabilitiesToChangeTarget().size();

		checkProbabilityIsNormalized();
		boolean inputIsValid = (totalProbabilities >= 1) && (totalProbabilities == totalTargets);

		if (!inputIsValid) {
			throw new IllegalArgumentException(String.format(
					"The size of \"probabilitiesToChangeTarget\" and \"nextTarget\" must be equal for %s algorithm", TargetChangerAlgorithmType.SORTED_SUB_LIST) );
		}
	}

	@Override
	public void init(Random rnd) {

		for (Double probability : targetChanger.getAttributes().getProbabilitiesToChangeTarget()) {
			JDKRandomGenerator randomGenerator = new JDKRandomGenerator();
			randomGenerator.setSeed(rnd.nextInt());

			binomialDistributions.add(new BinomialDistribution(randomGenerator, BINOMIAL_DISTRIBUTION_SUCCESS_VALUE, probability));
		}
	}

	@Override
	public boolean setAgentTargetList(Agent agent) {
		LinkedList<Integer> nextTargets = getNextTargets();
		boolean ret = false;
		if (nextTargets.size() > 0){
			agent.setTargets(nextTargets);
			agent.setNextTargetListIndex(0);
			agent.setIsCurrentTargetAnAgent(false);
			ret = true;
		}
		return ret;
	}

	private LinkedList<Integer> getNextTargets() {

		LinkedList<Integer> nextTargets = new LinkedList<>();

		for (int i = 0; i < binomialDistributions.size(); i++) {

			BinomialDistribution binomialDistribution = binomialDistributions.get(i);
			int binomialDistributionSample = binomialDistribution.sample();

			if (binomialDistributionSample == BINOMIAL_DISTRIBUTION_SUCCESS_VALUE) {
				nextTargets.add(targetChanger.getAttributes().getNextTarget().get(i));
			}
		}

		return nextTargets;
	}
}
