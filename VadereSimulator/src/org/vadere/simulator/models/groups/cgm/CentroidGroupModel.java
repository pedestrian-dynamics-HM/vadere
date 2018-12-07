package org.vadere.simulator.models.groups.cgm;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.groups.*;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.*;
import org.vadere.state.types.ScenarioElementType;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

@ModelClass
public class CentroidGroupModel
		implements GroupModel, DynamicElementAddListener<Pedestrian>, DynamicElementRemoveListener<Pedestrian> {

	private static Logger logger = LogManager.getLogger(CentroidGroupModel.class);

	private Random random;
	private Map<Integer, CentroidGroupFactory> groupFactories;		// for each source a separate group factory.
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

		// get all pedestrians already in topography
		DynamicElementContainer<Pedestrian> c = topography.getPedestrianDynamicElements();

		if (c.getInitialElements().size() > 0) {
			Map<Integer, List<Pedestrian>> groups = new HashMap<>();

			// aggregate group data
			c.getInitialElements().stream().forEach(p -> {
				for (Integer id : p.getGroupIds()) {
					List<Pedestrian> peds = groups.get(id);
					if (peds == null) {
						peds = new ArrayList<>();
						groups.put(id, peds);
					}
					// empty group id and size values, will be set later on
					p.setGroupIds(new LinkedList<>());
					p.setGroupSizes(new LinkedList<>());


					peds.add(p);
				}
			});


			// build groups depending on group ids and register pedestrian
			for (Integer id : groups.keySet()) {
				System.out.println("GroupId: " + id);
				List<Pedestrian> peds = groups.get(id);
				CentroidGroup group = getNewGroup(id, peds.size());
				peds.stream().forEach(p -> {
					// update group id / size info on ped
					p.getGroupIds().add(id);
					p.getGroupSizes().add(peds.size());
					group.addMember(p);
					registerMember(p, group);
				});
			}

			// set latest groupid to max id + 1
			Integer max = groups.keySet().stream().max(Integer::compareTo).get();
			nextFreeGroupId = new AtomicInteger(max+1);
		}
	}

	public void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget) {
		this.potentialFieldTarget = potentialFieldTarget;
		// update all existing groups
        for(CentroidGroup group : pedestrianGroupData.values()) {
            group.setPotentialFieldTarget(potentialFieldTarget);
        }
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
        logger.debug(String.format("Get Group for Pedestrian %s", ped));
        CentroidGroup group = pedestrianGroupData.get(ped);
        if(group == null) {
        	for (ScenarioElement p : pedestrianGroupData.keySet()) {
        		if (p.getId() == ped.getId()) {
        			group = pedestrianGroupData.get(p);
        			pedestrianGroupData.put(ped, group);
        			pedestrianGroupData.remove(p);
        			break;
				}
			}
		}
		return group;
	}

	@Override
	public void registerMember(final ScenarioElement ped, final Group group) {
	    logger.debug(String.format("Register Pedestrian %s, Group %s", ped, group));
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
		return getNewGroup(getFreeGroupId(), size);
	}

	private CentroidGroup getNewGroup(final int id, final int size) {
		return new CentroidGroup(id, size, this.potentialFieldTarget);
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
