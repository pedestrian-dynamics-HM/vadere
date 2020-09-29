package org.vadere.simulator.control.scenarioelements.targetchanger;


import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.TargetChangerAlgorithmType;
import org.vadere.state.scenario.TargetPedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class FollowPedestrianTargetChanger extends BaseTargetChangerAlgorithm {

	private BinomialDistribution binomialDistribution;

	public FollowPedestrianTargetChanger(TargetChanger targetChanger, Topography topography) {
		super(targetChanger, topography);
	}

	@Override
	public void throwExceptionOnInvalidInput(TargetChanger targetChanger) {
		int totalTargets = targetChanger.getAttributes().getNextTarget().size();
		int totalProbabilities = targetChanger.getAttributes().getProbabilitiesToChangeTarget().size();

		checkProbabilityIsNormalized();
		boolean inputIsValid = (totalProbabilities == 1 && totalTargets == 1);

		if (!inputIsValid) {
			throw new IllegalArgumentException(String.format(
					"The size of \"probabilitiesToChangeTarget\" must be 1 for. %s", TargetChangerAlgorithmType.FOLLOW_PERSON) );
		}
	}

	@Override
	public void init(Random rnd){
		binomialDistribution = createBinomialDistribution(rnd);
	}

	@Override
	public boolean setAgentTargetList(Agent agent) {

		// check probability
		if (binomialDistribution.sample() != BINOMIAL_DISTRIBUTION_SUCCESS_VALUE)
			return false;

		int nextTarget = (targetChanger.getAttributes().getNextTarget().size() > 0)
				? targetChanger.getAttributes().getNextTarget().get(0)
				: Attributes.ID_NOT_SET;

		Collection<Pedestrian> allPedestrians = topography.getElements(Pedestrian.class);
		List<Pedestrian> pedsWithCorrectTargetId = allPedestrians.stream()
				.filter(pedestrian -> pedestrian.getTargets().contains(nextTarget))
				.collect(Collectors.toList());

		if (pedsWithCorrectTargetId.size() > 0) {
			// Try to use a pedestrian which has already some followers
			// to avoid calculating multiple dynamic floor fields.
			List<Pedestrian> pedsWithFollowers = pedsWithCorrectTargetId.stream()
					.filter(pedestrian -> pedestrian.getFollowers().isEmpty() == false)
					.collect(Collectors.toList());

			Pedestrian pedToFollow = (pedsWithFollowers.isEmpty()) ? pedsWithCorrectTargetId.get(0) : pedsWithFollowers.get(0);
			agentFollowsOtherPedestrian(agent, pedToFollow);
		} else {

			useStaticTargetForAgent(agent, nextTarget);
		}
		return true;
	}

	private void agentFollowsOtherPedestrian(Agent agent, Pedestrian pedToFollow) {
		// Create the necessary TargetPedestrian wrapper object.
		// The simulation loop creates the corresponding controller objects
		// in the next simulation loop based on the exisiting targets in the topography.
		TargetPedestrian targetPedestrian = new TargetPedestrian(pedToFollow);
		topography.addTarget(targetPedestrian);

		// Make "agent" a follower of "pedToFollow".
		agent.setSingleTarget(targetPedestrian.getId(), true);
		pedToFollow.getFollowers().add(agent);
	}

	private void useStaticTargetForAgent(Agent agent, Integer nextTargets) {
		agent.setSingleTarget(nextTargets, false);
		agent.setNextTargetListIndex(0);
		agent.setIsCurrentTargetAnAgent(false);
	}
}
