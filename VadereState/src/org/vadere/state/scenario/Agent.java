package org.vadere.state.scenario;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.math.TruncatedNormalDistribution;

import java.util.*;

public abstract class Agent extends DynamicElement {

    // Member Variables
    private AttributesAgent attributes;

	private transient final List<AgentListener> listeners;
	private transient final HashMap<Class<? extends ScenarioElement>, Set<Integer>> encounteredScenarioElements;

	/**
	 * Source where the agent was spawned. The SourceController should
	 * set this field. It may be <code>null</code> when the agent is created
	 * in different way.
	 */
	private Source source;
	private LinkedList<Integer> targetIds;
    private int nextTargetListIndex;
    /** Indicates if current target is an agent or a (static) target. */
    private boolean isCurrentTargetAnAgent;

	private VPoint position;
	private Vector2D velocity;
	private double freeFlowSpeed;

	private LinkedList<Agent> followers;

	// TODO: Maybe, add also a List<Agent> of followers for an agent.

	// Constructors
	public Agent(AttributesAgent attributesAgent) {
        attributes = attributesAgent;

		position = new VPoint(0, 0);
		velocity = new Vector2D(0, 0);
		targetIds = new LinkedList<>();
		nextTargetListIndex = 0;
		isCurrentTargetAnAgent = false;

		followers = new LinkedList<>();
		encounteredScenarioElements = new HashMap<>();
		listeners = new LinkedList<>();
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
		this.isCurrentTargetAnAgent = other.isCurrentTargetAnAgent;

		this.position = other.position;
		this.velocity = other.velocity;
		this.freeFlowSpeed = other.freeFlowSpeed;

		this.setFollowers(new LinkedList<>(other.followers));
	}

	// Getters
    @Override
    public AttributesAgent getAttributes() {
        return attributes;
    }

    @Override
    public int getId() {
        return attributes.getId();
    }

	@Override
	public void setId(int id) {
		attributes.setId(id);
	}

	public Source getSource() {
        return source;
    }

    public LinkedList<Integer> getTargets() {
        return targetIds;
    }

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

    public boolean isCurrentTargetAnAgent() {
        return isCurrentTargetAnAgent;
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

	public LinkedList<Agent> getFollowers() {
		return followers;
	}

	// Setters
	@Override
	public void setAttributes(Attributes attributes) {
		this.attributes = (AttributesAgent) attributes;
	}

	public void setSource(Source source) {
		this.source = source;
	}

    public void setTargets(LinkedList<Integer> targetIds) {
        this.targetIds = targetIds;

		for (AgentListener listener: listeners) {
			listener.agentTargetsChanged(targetIds, this.getId());
		}
    }

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

    public void setIsCurrentTargetAnAgent(boolean isCurrentTargetAnAgent) {
        this.isCurrentTargetAnAgent = isCurrentTargetAnAgent;
    }

	public void setPosition(VPoint position) {
		this.position = position;
	}

	public void setVelocity(final Vector2D velocity) {
		this.velocity = velocity;
	}

	// TODO [task=refactoring] remove again!
	public void setFreeFlowSpeed(double freeFlowSpeed) {
		this.freeFlowSpeed = freeFlowSpeed;
	}

    @Override
    public void setShape(VShape newShape) {
        position = newShape.getCentroid();
    }

	// Methods
    public abstract Agent clone();

    /**
     *  Initially set pedestrians will not have source id set.
     */
    public boolean hasSource(){
        return source != null;
    }

    public void addTarget(Target target) {
        targetIds.add(target.getId());
    }

    public void incrementNextTargetListIndex() {
        // Deprecated target list usage
        if (nextTargetListIndex == -1) {
            throw new IllegalStateException("nextTargetListIndex is -1. this indicates the deprecated usage of "
                    + "the target list. you have to set the index to 0 before you can start incrementing.");
        }

        nextTargetListIndex++;
    }

    public boolean hasNextTarget() {
        // Deprecated target list usage
        if (nextTargetListIndex == -1) {
            return !targetIds.isEmpty();
        }

        // The right way:
        return nextTargetListIndex < targetIds.size();
    }

    public void setSingleTarget(int targetId, boolean targetIsAgent) {
    	LinkedList<Integer> nextTarget = new LinkedList<>();
    	nextTarget.add(targetId);

    	setTargets(nextTarget);
    	setNextTargetListIndex(0);
    	setIsCurrentTargetAnAgent(targetIsAgent);
	}

	// Static Methods
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

	public void setFollowers(LinkedList<Agent> followers) {
		this.followers = followers;
	}

	// TODO [priority=high] [task=deprecation] removing the target from the list is deprecated, but still very frequently used everywhere.
	public void checkNextTarget(double nextSpeed) {
		final int nextTargetListIndex = this.getNextTargetListIndex();

		// Deprecated target list usage
		if (nextTargetListIndex <= -1 && !this.getTargets().isEmpty()) {
			this.getTargets().removeFirst();
		}

		// The right way (later this first check should not be necessary anymore):
		if (this.hasNextTarget()) {
			this.incrementNextTargetListIndex();
			for (AgentListener listener: listeners) {
				listener.agentNextTargetSet(nextSpeed, this.getId());
			}
		}

		// set a new desired speed, if possible. you can model street networks with differing
		// maximal speeds with this.
		if (nextSpeed >= 0) {
			this.setFreeFlowSpeed(nextSpeed);
		}
	}

	public <T extends ScenarioElement> void elementEncountered(Class<T> clazz, T elem) {
		encounteredScenarioElements.computeIfAbsent(clazz, k -> new HashSet<>())
						.add(elem.getId());
		for (AgentListener listener: listeners) {
			listener.agentElementEncountered(elem, this.getId());
		}
	}

	public <T extends ScenarioElement> Set<Integer> getElementsEncountered(Class<T> clazz) {
		return encounteredScenarioElements.getOrDefault(clazz, new HashSet<>());
	}

	public void clearListeners() {
		this.listeners.clear();
	}

	public void addAgentListener(AgentListener listener) {
		this.listeners.add(listener);
	}

}
