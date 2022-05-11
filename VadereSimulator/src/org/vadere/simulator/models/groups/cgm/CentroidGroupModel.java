package org.vadere.simulator.models.groups.cgm;


import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.groups.AbstractGroupModel;
import org.vadere.simulator.models.groups.Group;
import org.vadere.simulator.models.groups.GroupSizeDeterminator;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElementContainer;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Implementation of group behavior model described in 'Pedestrian Group Behavior in a Cellular
 * Automaton' (bib-key: seitz-2014). The basic idea from seitz-2014:
 * <quote>
 * Firstly members walking ahead slow down and members falling behind slightly speed up to reach the group.
 * </quote>
 *
 * Agents which are added to the topography (before the simulation starts) are assigned to
 * groups directly in the {@link #preLoop(double)}. Agents which are spawned later on, are
 * assigned to groups in the callback {@link #elementAdded(Pedestrian)}. The actual behavior
 * of the central group model is implemented in the helper class {@link CentroidGroup}.
 */
@ModelClass
public class CentroidGroupModel extends AbstractGroupModel<CentroidGroup> {

	private Random random;
	private LinkedHashMap<Integer, CentroidGroup> groupsById;
	private Map<Integer, LinkedList<CentroidGroup>> sourceNextGroups;
	private Map<Integer, GroupSizeDeterminator> sourceGroupSizeDeterminator;

	//TODO currently as member because outside functions want to manipulate it without manipulating the group
	private Map<Integer, LinkedList<Integer>> pedestrianGroupSizes;

	private Topography topography;
	private IPotentialFieldTarget potentialFieldTarget;
	private AttributesCGM attributesCGM;

	private AtomicInteger nextFreeGroupId;

	public CentroidGroupModel() {
//		this.pedestrianGroupMap = new HashMap<>();
		this.groupsById = new LinkedHashMap<>();
		this.sourceNextGroups = new HashMap<>();
		this.sourceGroupSizeDeterminator = new HashMap<>();

		this.nextFreeGroupId = new AtomicInteger(0);
	}

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {
		this.attributesCGM = Model.findAttributes(attributesList, AttributesCGM.class);
		this.topography = domain.getTopography();
		this.random = random;

	}

	@Override
	public void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget) {
		this.potentialFieldTarget = potentialFieldTarget;
		// update all existing groups
		for (CentroidGroup group : groupsById.values()) {
			group.setPotentialFieldTarget(potentialFieldTarget);
		}
	}

	@Override
	public IPotentialFieldTarget getPotentialFieldTarget() {
		return potentialFieldTarget;
	}

	private int getFreeGroupId() {
		return nextFreeGroupId.getAndIncrement();
	}


	@Override
	public void registerGroupSizeDeterminator(int sourceId, GroupSizeDeterminator gsD) {
		sourceGroupSizeDeterminator.put(sourceId, gsD);
		sourceNextGroups.put(sourceId, new LinkedList<>());
	}

	@Override
	public int nextGroupForSource(int sourceId) {
		GroupSizeDeterminator gsD = sourceGroupSizeDeterminator.get(sourceId);
		assert gsD != null : "GroupSizeDeterminator not initialized for source";

		CentroidGroup newGroup = getNewGroup(gsD.nextGroupSize());
		LinkedList<CentroidGroup> groups = sourceNextGroups.get(sourceId);
		assert groups != null : "SourceNextGroupMap not initialized for group";
		groups.addLast(newGroup);

		return newGroup.getSize();
	}

	@Override
	public CentroidGroup getGroup(final Pedestrian pedestrian) {

		/*Optional<CentroidGroup> group = groupsById.values().stream()
				.filter(g -> g.members.contains(pedestrian))
				.findFirst();
		assert group.isPresent() : "No group found for pedestrian";
		return group.get();*/

		CentroidGroup group = groupsById.get(pedestrian.getGroupIds().getFirst());
		assert group != null : "No group found for pedestrian";
		return group;
	}

	@Override
	protected void registerMember(final Pedestrian ped, final CentroidGroup group) {
		//TODO does not register the pedestrian but the group. and only the first group of the pedestrian
		groupsById.putIfAbsent(ped.getGroupIds().getFirst(), group);
	}

	protected void registerGroup(final CentroidGroup group) {
		groupsById.putIfAbsent(group.getID(), group);
	}

	@Override
	public Map<Integer, CentroidGroup> getGroupsById() {
		return groupsById;
	}

	@Override
	protected CentroidGroup getNewGroup(final int size) {
		return getNewGroup(getFreeGroupId(), size);
	}

	@Override
	protected CentroidGroup getNewGroup(final int id, final int size) {
		return new CentroidGroup(id, size, this);
	}

	private void initializeGroupsOfInitialPedestrians() {
		// get all pedestrians already in topography
		DynamicElementContainer<Pedestrian> c = topography.getPedestrianDynamicElements();

		if (c.getElements().size() > 0) {
			Map<Integer, List<Pedestrian>> groups = new HashMap<>();

			// aggregate group data
			c.getElements().forEach(p -> {
				for (Integer id : p.getGroupIds()) {
					List<Pedestrian> peds = groups.computeIfAbsent(id, k -> new ArrayList<>());
					// empty group id and size values, will be set later on
					p.setGroupIds(new LinkedList<>());
					p.setGroupSizes(new LinkedList<>());
					peds.add(p);
				}
			});


			// build groups depending on group ids and register pedestrian
			for (Integer id : groups.keySet()) {
				List<Pedestrian> peds = groups.get(id);
				CentroidGroup group = getNewGroup(id, peds.size());
				peds.forEach(p -> {
					// update group id / size info on ped
					p.getGroupIds().add(id);
					p.getGroupSizes().add(peds.size());
					group.addMember(p);
					registerMember(p, group);
				});
			}

			// set latest groupid to max id + 1
			Optional<Integer> max = groups.keySet().stream().max(Integer::compareTo);
			if (max.isPresent()) {
				nextFreeGroupId = new AtomicInteger(max.get() + 1);
			}
		}
	}


	protected void assignToGroup(Pedestrian ped) {
		// get first group for current source.
		LinkedList<CentroidGroup> groups = sourceNextGroups.get(ped.getSource().getId());
		CentroidGroup currentGroup = groups.peekFirst();
		if (currentGroup == null) {
			throw new IllegalStateException("No empty group exists to add Pedestrian: " + ped.getId());
		}
		currentGroup.addMember(ped);
		addGroupSize(ped.getId(), currentGroup.getSize());
		//If group is full remove it from the sourceNextGroups list.
		if (currentGroup.getOpenPersons() == 0) {
			groups.pollFirst(); // remove full group from list.
		}
		registerMember(ped, currentGroup);
	}

	public AttributesCGM getAttributesCGM() {
		return attributesCGM;
	}

	/* DynamicElement Listeners */

	@Override
	public void elementAdded(Pedestrian pedestrian) {
		assignToGroup(pedestrian);
	}

	@Override
	public void elementRemoved(Pedestrian pedestrian) {
		Group group = groupsById.get(pedestrian.getGroupIds().getFirst());
		if (group.removeMember(pedestrian)) { // if true pedestrian was last member.
			groupsById.remove(group.getID());
		}
	}

	/* Model Interface */

	@Override
	public void preLoop(final double simTimeInSec) {
		initializeGroupsOfInitialPedestrians();
		topography.addElementAddedListener(Pedestrian.class, this);
		topography.addElementRemovedListener(Pedestrian.class, this);
	}

	@Override
	public void postLoop(final double simTimeInSec) {
	}

	@Override
	public void update(final double simTimeInSec) {
	}


	protected Topography getTopography() {
		return topography;
	}

	@Override
	public void setGroupIds(List<Integer> groupIds, Pedestrian ped) {
		//remove existing group memberships
		for (int groupId: getGroupIds(ped)) {
			groupsById.get(groupId).removeMember(ped);
		}

		//set new group memberships
		LinkedList<Integer> groupSizes = new LinkedList<>();
		for (int groupId: groupIds) {
			CentroidGroup groupToInsert = groupsById.get(groupId);
			if (groupToInsert == null) {
				groupToInsert = getNewGroup(groupId, (int) getAverageGroupSize());
			}
			groupToInsert.addMemberInAnyCase(ped);
			groupSizes.add(groupToInsert.getSize());
			//If group is full remove it from the sourceNextGroups list in case it is in there
			if (groupToInsert.getOpenPersons() == 0) {
				CentroidGroup finalGroupToInsert = groupToInsert;
				sourceNextGroups.values()
						.forEach((groupList) -> {
							groupList.remove(finalGroupToInsert);});
			}

			setGroupSizes(groupSizes, ped);
			registerGroup(groupToInsert);
		}
	}

	private double getAverageGroupSize() {
		return groupsById.values().stream()
				.map(group -> group.getSize())
				.mapToInt(Integer::intValue)
				.average()
				.orElse(0.0);
	}

	@Override
	public LinkedList<Integer> getGroupIds(Pedestrian ped) {
		return groupsById.values()
				.stream()
				.filter(group -> group.isMember(ped))
				.map(CentroidGroup::getID)
				.collect(Collectors.toCollection(LinkedList::new));
	}

	@Override
	public List<Integer> getGroupSizes(Pedestrian ped) {
		return pedestrianGroupSizes.get(ped.getId());
	}

	@Override
	public void setGroupSizes(LinkedList<Integer> groupSizes, Pedestrian ped) {
		pedestrianGroupSizes.put(ped.getId(), groupSizes);
	}

	public void addGroupSize(int pedestrianId, int groupSize) {
		LinkedList<Integer> pedGroupSizes = pedestrianGroupSizes.getOrDefault(pedestrianId, new LinkedList<>());
		pedGroupSizes.add(groupSize);
		pedestrianGroupSizes.put(pedestrianId, pedGroupSizes);
	}

	@Override
	public List<Pedestrian> getPedGroupMembers(Pedestrian ped) {
		CentroidGroup group = groupsById.get(getGroupIds(ped).getFirst());
		return group.members.stream()
				.filter(p -> p.getId() != ped.getId())
				.collect(Collectors.toList());
	}

	@Override
	public void setAgentsInGroup(List<Pedestrian> agentsInGroup, Pedestrian ped) {
		CentroidGroup group = groupsById.get(getGroupIds(ped).getFirst());
		group.members.clear();
		for (Pedestrian pedestrian: agentsInGroup) {
			group.addMemberInAnyCase(pedestrian);
		}
	}
}
