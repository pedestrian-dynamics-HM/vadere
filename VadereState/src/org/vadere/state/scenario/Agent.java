package org.vadere.state.scenario;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.TruncatedNormalDistribution;

public abstract class Agent implements DynamicElement {
	
	/**
	 * Source where the agent was spawned. The {@link SourceController} should
	 * set this field. It may be <code>null</code> when the agent is created
	 * in different way.
	 */
	private Source source;

	private LinkedList<Integer> targetIds;
	private VPoint position;
	private Vector2D velocity;

	private int nextTargetListIndex;

	private double freeFlowSpeed;

	private AttributesAgent attributes;

	public Agent(AttributesAgent attributesAgent) {
		position = new VPoint(0, 0);
		velocity = new Vector2D(0, 0);
		targetIds = new LinkedList<>();
		nextTargetListIndex = 0;

		attributes = attributesAgent;
	}

	public Agent(AttributesAgent attributesAgent, Random random) {
		this(attributesAgent);

		if (attributesAgent.getSpeedDistributionStandardDeviation() == 0) {
			freeFlowSpeed = attributesAgent.getSpeedDistributionMean();
		} else {
			final RandomGenerator rng = new JDKRandomGenerator(random.nextInt());
			final TruncatedNormalDistribution speedDistribution = new TruncatedNormalDistribution(rng,
					attributesAgent.getSpeedDistributionMean(),
					attributesAgent.getSpeedDistributionStandardDeviation(),
					attributesAgent.getMinimumSpeed(),
					attributesAgent.getMaximumSpeed(),
					100);
			freeFlowSpeed = speedDistribution.sample();
		}
	}

	public Agent(Agent other) {
		this(other.attributes);

		this.setTargets(new LinkedList<>(other.targetIds));
		this.setNextTargetListIndex(other.nextTargetListIndex);

		this.setPosition(other.position);
		this.setVelocity(other.velocity);
		this.setFreeFlowSpeed(other.freeFlowSpeed);
	}

	public LinkedList<Integer> getTargets() {
		return targetIds;
	}

	public Vector2D getVelocity() {
		return velocity;
	}

	public double getFreeFlowSpeed() {
		return freeFlowSpeed;
	}

	public double getSpeedDistributionMean() {
		return attributes.getSpeedDistributionMean();
	}

	public double getAcceleration() {
		return attributes.getAcceleration();
	}

	public double getRadius() {
		return attributes.getRadius();
	}

	@Override
	public VPoint getPosition() {
		return position;
	}

	@Override
	public VShape getShape() {
		return new VCircle(position, attributes.getRadius());
	}

	public Source getSource() {
		return source;
	}

	@Override
	public int getId() {
		return attributes.getId();
	}

	@Override
	public abstract Agent clone();

	/**
	 * Converts a Iterable of Agent to a List of VPoint positions.
	 * 
	 * @param agents
	 * @return a List of VPoint positions of the agents
	 */
	public static List<VPoint> getPositions(final Iterable<Agent> agents) {
		List<VPoint> agentPositions = new ArrayList<>();
		if (agents != null) {
			for (Agent agent : agents) {
				agentPositions.add(agent.getPosition());
			}
		}

		return agentPositions;
	}

	// Getters...

	/**
	 * Get the index pointing to the next target in the target list.
	 * 
	 * Usually this index is >= 0 and <= {@link #getTargets()}<code>.size()</code>. Targets are
	 * never removed from the target list. This index is incremented instead.
	 * 
	 * In deprecated usage this index is -1. This means, the next target is always the first target
	 * in the list. Once a target is reached it is remove from the list.
	 * 
	 */
	public int getNextTargetListIndex() {
		return nextTargetListIndex;
	}

	/**
	 * Get the id of the next target. Please call {@link #hasNextTarget()} first, to check if there
	 * is a next target. If there is no next target, an exception is thrown.
	 * 
	 */
	public int getNextTargetId() {
		// Deprecated target list usage
		if (nextTargetListIndex == -1) {
			return targetIds.getFirst();
		}

		// The right way:
		return targetIds.get(nextTargetListIndex);
	}

	public boolean hasNextTarget() {
		// Deprecated target list usage
		if (nextTargetListIndex == -1) {
			return !targetIds.isEmpty();
		}

		// The right way:
		return nextTargetListIndex < targetIds.size();
	}

	@Override
	public AttributesAgent getAttributes() {
		return attributes;
	}

	// Setters...


	/**
	 * Set the index pointing to the next target in the target list.
	 * 
	 * Set the index to 0 to set the first target in the target list as next target. Use
	 * {@link #incrementNextTargetListIndex()} to proceed to the next target.
	 * 
	 * Set the index to -1 if you really have to use the deprecated target list approach.
	 * 
	 * @see #getNextTargetListIndex()
	 */
	public void setNextTargetListIndex(int nextTargetListIndex) {
		this.nextTargetListIndex = nextTargetListIndex;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public void incrementNextTargetListIndex() {
		// Deprecated target list usage
		if (nextTargetListIndex == -1) {
			throw new IllegalStateException("nextTargetListIndex is -1. this indicates the deprecated usage of "
					+ "the target list. you have to set the index to 0 before you can start incrementing.");
		}

		nextTargetListIndex++;
	}

	public void setPosition(VPoint position) {
		this.position = position;
	}

	public void setVelocity(final Vector2D velocity) {
		this.velocity = velocity;
	}

	public void setTargets(LinkedList<Integer> targetIds) {
		this.targetIds = targetIds;
	}

	// TODO [task=refactoring] remove again!
	public void setFreeFlowSpeed(double freeFlowSpeed) {
		this.freeFlowSpeed = freeFlowSpeed;
	}

}
