package org.vadere.simulator.models.groups;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElementAddListener;
import org.vadere.state.scenario.DynamicElementRemoveListener;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Topography;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

@ModelClass
public class CentroidGroupModel
		implements GroupModel, DynamicElementAddListener<Pedestrian>, DynamicElementRemoveListener<Pedestrian> {

	private Random random;
	private Map<Integer, CentroidGroupFactory> groupFactories;
	private Map<ScenarioElement, CentroidGroup> pedestrianGroupData;

	private Topography topography;
	private IPotentialFieldTarget potentialFieldTarget;
	private AttributesCGM attributesCGM;

	private AtomicInteger nextFreeGroupId;

	public CentroidGroupModel() {
		this.groupFactories = new HashMap<>();
		this.pedestrianGroupData = new HashMap<>();
		this.nextFreeGroupId = new AtomicInteger(0);
	}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
						   AttributesAgent attributesPedestrian, Random random) {
		this.attributesCGM = Model.findAttributes(attributesList, AttributesCGM.class);
		this.topography = topography;
		this.random = random;
//		setGroupSizeDeterminator(new GroupSizeDeterminatorRandom(
//				attributesCGM.getGroupSizeDistribution(), random));
	}

	public void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget) {
		this.potentialFieldTarget = potentialFieldTarget;
	}

	protected int getFreeGroupId() {
		return nextFreeGroupId.getAndIncrement();
	}

	@Override
	public GroupFactory getGroupFactory(final int sourceId) {

		CentroidGroupFactory result = groupFactories.get(sourceId);

		if (result == null) {
			throw new IllegalArgumentException("For SourceID: " + sourceId + " no GroupFactory exists. " +
					"Is this really a valid source?");
		}

		return result;
	}

	@Override
	public void initializeGroupFactory(int sourceId, List<Double> groupSizeDistribution) {
		GroupSizeDeterminator gsD = new GroupSizeDeterminatorRandom(groupSizeDistribution, random);
		CentroidGroupFactory result =
				new CentroidGroupFactory(this, gsD);
		groupFactories.put(sourceId, result);
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
	public CentroidGroup removeMember(ScenarioElement ped) {
		return pedestrianGroupData.remove(ped);
	}


	public Map<ScenarioElement, CentroidGroup> getPedestrianGroupData() {
		return pedestrianGroupData;
	}

	@Override
	public CentroidGroup getNewGroup(final int size) {
		CentroidGroup result = new CentroidGroup(getFreeGroupId(), size,
				this.potentialFieldTarget);
		return result;
	}

	@Override
	public void preLoop(final double simTimeInSec) {
		topography.addElementAddedListener(Pedestrian.class, this);
		topography.addElementRemovedListener(Pedestrian.class, this);
	}

	@Override
	public void postLoop(final double simTimeInSec) {
	}

	@Override
	public void update(final double simTimeInSec) {
	}

	public AttributesCGM getAttributesCGM() {
		return attributesCGM;
	}

	@Override
	public void elementAdded(Pedestrian pedestrian) {
		// call GroupFactory for selected Source
		getGroupFactory(pedestrian.getSource().getId()).elementAdded(pedestrian);
	}

	@Override
	public void elementRemoved(Pedestrian pedestrian) {
		getGroupFactory(pedestrian.getSource().getId()).elementRemoved(pedestrian);
	}

}
