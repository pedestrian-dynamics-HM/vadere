package org.vadere.state.scenario;

import java.util.*;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

public class Pedestrian extends Agent {

	/** Target ID if the pedestrian represents a target, -1 otherwise. */
	private int idAsTarget; // TODO should actually be an attribute or a member of a subclass
	private Map<Class<? extends ModelPedestrian>, ModelPedestrian> modelPedestrianMap;

	private boolean isChild; // TODO should actually be an attribute or a member of a subclass
	private boolean isLikelyInjured; // TODO should actually be an attribute or a member of a subclass

	private LinkedList<Integer> groupIds; // TODO should actually be an attribute or a member of a subclass

	/** Used only for JSON serialization? */
	// TODO used at all? Car does NOT have this field. remove if unused!
	private ScenarioElementType type = ScenarioElementType.PEDESTRIAN;

	@SuppressWarnings("unused")
	private Pedestrian() {
		// TODO constructor may be required for Jackson?
		this(new AttributesAgent());
	}

	private Pedestrian(AttributesAgent attributesPedestrian) {
		this(attributesPedestrian, new Random());
	}

	public Pedestrian(AttributesAgent attributesAgent, Random random) {
		super(attributesAgent, random);

		modelPedestrianMap = new HashMap<>();

		idAsTarget = -1;
		isChild = false;
		isLikelyInjured = false;
		groupIds = new LinkedList<>();
	}

	/**
	 * Copy constructor, references the same attributes.
	 */
	private Pedestrian(Pedestrian other) {
		super(other);
		modelPedestrianMap = new HashMap<>(other.modelPedestrianMap);
		isChild = other.isChild;
		isLikelyInjured = other.isLikelyInjured;

		idAsTarget = other.idAsTarget;

		if (other.groupIds != null) {
			groupIds = new LinkedList<>(other.groupIds);
		} else {
			groupIds = new LinkedList<>();
		}
	}

	public <T extends ModelPedestrian> T getModelPedestrian(Class<? extends T> modelType) {
		return (T) modelPedestrianMap.get(modelType);
	}

	public <T extends ModelPedestrian> ModelPedestrian setModelPedestrian(T modelPedestrian) {
		return modelPedestrianMap.put(modelPedestrian.getClass(), modelPedestrian);
	}

	public void setGroupIds(LinkedList<Integer> groupIds) {
		this.groupIds = groupIds;
	}

	public VShape getInformationShape() {
		return null;
	}

	public LinkedList<Integer> getGroupIds() {
		return groupIds;
	}

	public boolean isTarget() {
		return this.idAsTarget != -1;
	}

	public int getIdAsTarget() {
		return this.idAsTarget;
	}

	public void setIdAsTarget(int id) {
		this.idAsTarget = id;
	}

	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.PEDESTRIAN;
	}

	public boolean isChild() {
		return isChild;
	}

	public void setChild(boolean child) {
		this.isChild = child;
	}

	public boolean isLikelyInjured() {
		return isLikelyInjured;
	}

	public void setLikelyInjured(boolean likelyInjured) {
		this.isLikelyInjured = likelyInjured;
	}

	@Override
	public Pedestrian clone() {
		return new Pedestrian(this);
	}

}
