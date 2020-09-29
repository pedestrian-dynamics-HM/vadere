package org.vadere.simulator.control.scenarioelements.targetchanger;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.simulator.control.scenarioelements.TargetChangerController;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.TargetChangerAlgorithmType;
import org.vadere.state.scenario.Topography;

import java.util.Random;

public interface TargetChangerAlgorithm {

	int BINOMIAL_DISTRIBUTION_SUCCESS_VALUE = 1;

	static TargetChangerAlgorithm create(TargetChanger targetChanger, Topography topography){
		TargetChangerAlgorithmType type = targetChanger.getAttributes().getChangeAlgorithmType();
		TargetChangerAlgorithm algorithm = null;
		switch (type){
			case SELECT_LIST:
				algorithm = new SelectListTargetChanger(targetChanger, topography);
				break;
			case FOLLOW_PERSON:
				algorithm = new FollowPedestrianTargetChanger(targetChanger, topography);
				break;
			case SELECT_ELEMENT:
				algorithm = new SelectElementTargetChanger(targetChanger, topography);
				break;
			case SORTED_SUB_LIST:
				algorithm = new SortedSubListTargetChanger(targetChanger, topography);
				break;
			default:
				throw new RuntimeException("Unkonwn TargetChangerAlgorithm");
		}
		return algorithm;
	}

	/** Ensure setup of TargetChanger is correct for given algorithm*/
	void throwExceptionOnInvalidInput(TargetChanger targetChanger);

	void init(Random rnd);


	TargetChanger getTargetChanger();

	default BinomialDistribution createBinomialDistribution(Random rnd){
		JDKRandomGenerator randomGenerator = new JDKRandomGenerator();
		randomGenerator.setSeed(rnd.nextInt());
		double probability = getTargetChanger().getAttributes().getProbabilitiesToChangeTarget().getFirst();
		return new BinomialDistribution(randomGenerator, BINOMIAL_DISTRIBUTION_SUCCESS_VALUE, probability);
	}

	/**
	 * Set target list of agent  based on algorithm.
	 *
	 * Return true if targets have been changed, otherwise false
	 */
	boolean setAgentTargetList(Agent agent);
}
