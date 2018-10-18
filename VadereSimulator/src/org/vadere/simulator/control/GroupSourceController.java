package org.vadere.simulator.control;

import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.simulator.models.groups.GroupModel;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.SpawnArray;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class GroupSourceController extends SourceController {

	private final GroupModel groupModel;
	private LinkedList<Integer> groupsToSpawn;
	protected final SpawnArray spawnArray;

	public GroupSourceController(Topography scenario, Source source,
								 DynamicElementFactory dynamicElementFactory,
								 AttributesDynamicElement attributesDynamicElement,
								 Random random, GroupModel groupModel) {
		super(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
		this.groupModel = groupModel;
		this.groupsToSpawn = new LinkedList<>();

		VRectangle elementBound = new VRectangle(dynamicElementFactory.getDynamicElementRequiredPlace(new VPoint(0,0)).getBounds2D());

		this.spawnArray = new SpawnArray(new VRectangle(source.getShape().getBounds2D()),
				new VRectangle(0, 0,elementBound.getWidth() , elementBound.getHeight() ));

	}

	@Override
	public void update(double simTimeInSec) {
		if (!isSourceFinished(simTimeInSec)) {
			if (simTimeInSec >= timeOfNextEvent || !groupsToSpawn.isEmpty()) {
				determineNumberOfSpawnsAndNextEvent(simTimeInSec);

				if (sourceAttributes.isSpawnAtRandomPositions()) {

					if (sourceAttributes.isUseFreeSpaceOnly()) {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextFreeGroup(groupSize, random, getDynElementsAtSource());
							if (newGroup.size() > 0) {
								// add immediately to Scenario to update DynElementsAtSource
								addElementToScenario(newGroup);
								iter.remove();
							} else {
								break; // FIFO Spawn. The rest of the queue at next time step
							}
						}
					} else {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextGroup(groupSize, random, getDynElementsAtSource());
							if (newGroup.isEmpty())
								throw new RuntimeException("Cannot spawn new Group. Source " + source.getId() + " is set " +
										"to useFreeSpaceOnly == false but no space is left to spawn group without exactly" +
										"overlapping with neighbours which can cause numerical problems. Use useFreeSpaceOnly == true (default)" +
										"to queue groups.");
							addElementToScenario(newGroup);
							iter.remove();
						}
					}

				} else {

					if (sourceAttributes.isUseFreeSpaceOnly()) {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextFreeGroup(groupSize, getDynElementsAtSource());
							if (newGroup != null && !newGroup.isEmpty()) {
								// add immediately to Scenario to update DynElementsAtSource
								addElementToScenario(newGroup);
								iter.remove();
							} else {
								break; // FIFO Spawn. The rest of the queue at next time step
							}
						}
					} else {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextGroup(groupSize, getDynElementsAtSource());
							if (newGroup == null || newGroup.isEmpty())
								throw new RuntimeException("Cannot spawn new Group. Source " + source.getId() + " is set " +
										"to useFreeSpaceOnly == false but no space is left to spawn group without exactly" +
										"overlapping with neighbours which can cause numerical problems. Use useFreeSpaceOnly == true (default)" +
										"to queue groups.");
							addElementToScenario(newGroup);
							iter.remove();
						}
					}

				}


			}
		}
	}

	private void addElementToScenario(List<VPoint> group) {
		if (!group.isEmpty() && !isMaximumNumberOfSpawnedElementsReached()) {
			addNewAgentToScenario(group);
			dynamicElementsCreatedTotal += group.size();
		}
	}

	private void getNewGroupSizeFromModel() {
		for (int i = 0; i < sourceAttributes.getSpawnNumber(); i++) {
			int groupSize = groupModel.getGroupFactory(source.getId()).createNewGroup();
			groupsToSpawn.add(groupSize);
		}
	}


	@Override
	protected boolean isQueueEmpty() {
		return false;
	}

	@Override
	protected void determineNumberOfSpawnsAndNextEvent(double simTimeInSec) {
		while (timeOfNextEvent <= simTimeInSec) {
			getNewGroupSizeFromModel();
			createNextEvent();
		}
	}
}
