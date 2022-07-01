package org.vadere.state.attributes.models;

import java.util.Arrays;
import java.util.List;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesCGM extends Attributes {

	private double groupMemberRepulsionFactor = 0.01;
	private double leaderAttractionFactor = 0.003;
	private boolean lostMembers = false;

	private int waitBehaviourRelevantAgentsFactor = -1;

	/**
	 *  moved to org.vadere.state.attributes.scenario.AttributeSource class to allow
	 *  different groupDistributions for each source
	 */
//	@Deprecated()
//	private List<Double> groupSizeDistribution = Arrays.asList(0.0, 0.0, 1.0);

	public double getGroupMemberRepulsionFactor() {
		return groupMemberRepulsionFactor;
	}

	public double getLeaderAttractionFactor() {
		return leaderAttractionFactor;
	}

	public boolean isLostMembers(){ return lostMembers; }

	public int getWaitBehaviourRelevantAgentsFactor() {
		return waitBehaviourRelevantAgentsFactor;
	}

	public void setWaitBehaviourRelevantAgentsFactor(int waitBehaviourRelevantAgentsFactor) {
		this.waitBehaviourRelevantAgentsFactor = waitBehaviourRelevantAgentsFactor;
	}
}
