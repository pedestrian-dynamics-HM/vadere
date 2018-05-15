package org.vadere.simulator.models.groups;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Topography;

@ModelClass
public class CentroidGroupModel implements GroupModel {

	private GroupSizeDeterminator groupSizeDeterminator;
	private Map<Integer, CentroidGroupFactory> groupFactories;
	private Map<ScenarioElement, CentroidGroup> pedestrianGroupData;

	private Topography topography;
	private IPotentialFieldTarget potentialFieldTarget;
	private AttributesCGM attributesCGM;

	private int nextFreeGroupId = 0;
	
	public CentroidGroupModel() {
		this.groupFactories = new TreeMap<>();
		this.pedestrianGroupData = new HashMap<>();
	}
	
	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		this.attributesCGM = Model.findAttributes(attributesList, AttributesCGM.class);
		this.groupSizeDeterminator = new GroupSizeDeterminatorRandom(
				attributesCGM.getGroupSizeDistribution(), random);
		this.topography = topography;
	}

	public void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget) {
		this.potentialFieldTarget = potentialFieldTarget;
	}

	protected int getFreeGroupId() {
		int result = nextFreeGroupId;

		nextFreeGroupId++;

		return result;
	}

	@Override
	public GroupFactory getGroupFactory(final int sourceId) {

		CentroidGroupFactory result = groupFactories.get(sourceId);

		if (result == null) {
			result = new CentroidGroupFactory(this, groupSizeDeterminator);
			groupFactories.put(sourceId, result);
		}

		return result;
	}

	@Override
	public CentroidGroup getGroup(final ScenarioElement ped) {
		return pedestrianGroupData.get(ped);
	}

	@Override
	public void registerMember(final ScenarioElement ped, final Group group) {
		pedestrianGroupData.put(ped, (CentroidGroup) group);
	}

	@Override
	public CentroidGroup getNewGroup(final int size) {
		CentroidGroup result = new CentroidGroup(getFreeGroupId(), size,
				this.potentialFieldTarget);
		return result;
	}

	@Override
	public void preLoop(final double simTimeInSec) {
		topography.addElementAddedListener(Pedestrian.class, getGroupFactory(-1));
	}

	@Override
	public void postLoop(final double simTimeInSec) {}

	@Override
	public void update(final double simTimeInSec) {}

	public AttributesCGM getAttributesCGM() {
		return attributesCGM;
	}
	
}
