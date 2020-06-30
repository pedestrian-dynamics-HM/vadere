package org.vadere.simulator.control.scenarioelements.targetchanger;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.TargetChangerAlgorithmType;
import org.vadere.state.scenario.Topography;

import java.util.Random;

public class SelectListTargetChanger extends BaseTargetChangerAlgorithm {

	private BinomialDistribution binomialDistribution;

	public SelectListTargetChanger(TargetChanger targetChanger, Topography topography) {
		super(targetChanger, topography);
	}

	@Override
	public void throwExceptionOnInvalidInput(TargetChanger targetChanger) {
		int totalTargets = targetChanger.getAttributes().getNextTarget().size();
		int totalProbabilities = targetChanger.getAttributes().getProbabilitiesToChangeTarget().size();

		checkProbabilityIsNormalized();
		boolean inputIsValid = (totalProbabilities == 1 && totalTargets > 0);

		if (!inputIsValid) {
			throw new IllegalArgumentException(String.format(
					"The size of \"probabilitiesToChangeTarget\" must be 1 and nextTarget must be set. %s", TargetChangerAlgorithmType.SELECT_LIST) );
		}
	}

	@Override
	public void init(Random rnd) {
		binomialDistribution = createBinomialDistribution(rnd);
	}

	@Override
	public boolean setAgentTargetList(Agent agent) {

		if (binomialDistribution.sample() != BINOMIAL_DISTRIBUTION_SUCCESS_VALUE)
			return false;

		agent.setTargets(targetChanger.getAttributes().getNextTarget());
		agent.setNextTargetListIndex(0);
		agent.setIsCurrentTargetAnAgent(false);
		return true;
	}
}
