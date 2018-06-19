package org.vadere.simulator.control;

import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.simulator.models.groups.GroupModel;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class GroupSourceController extends SourceController {

	private final GroupModel groupModel;
	private LinkedList<Integer> groupsToSpawn;

	public GroupSourceController(Topography scenario, Source source,
								 DynamicElementFactory dynamicElementFactory,
								 AttributesDynamicElement attributesDynamicElement,
								 Random random, GroupModel groupModel) {
		super(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
		this.groupModel = groupModel;
		this.groupsToSpawn = new LinkedList<>();
	}

	@Override
	public void update(double simTimeInSec) {
		if (!isSourceFinished(simTimeInSec)) {
			if (simTimeInSec >= timeOfNextEvent || !groupsToSpawn.isEmpty()) {
				determineNumberOfSpawnsAndNextEvent(simTimeInSec);
				LinkedList<List<VPoint>> spawnGroups = new LinkedList<>();

				if (sourceAttributes.isSpawnAtRandomPositions()) {

					if (sourceAttributes.isUseFreeSpaceOnly()) {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextFreeGroup(groupSize, random, getDynElementsAtSource());
							if (newGroup != null) {
								spawnGroups.add(newGroup);
								iter.remove();
							} else {
								break; // FIFO Spawn. The rest of the queue at next time step
							}
						}
					} else {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextGroup(groupSize, random);
							spawnGroups.add(newGroup);
							iter.remove();
						}
					}

				} else {

					if (sourceAttributes.isUseFreeSpaceOnly()) {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextFreeGroup(groupSize, getDynElementsAtSource());
							if (newGroup != null) {
								spawnGroups.add(newGroup);
								iter.remove();
							} else {
								break; // FIFO Spawn. The rest of the queue at next time step
							}
						}
					} else {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextGroup(groupSize);
							spawnGroups.add(newGroup);
							iter.remove();
						}
					}

				}

				for (List<VPoint> group : spawnGroups) {
					if (!isMaximumNumberOfSpawnedElementsReached()) {
						addNewAgentToScenario(group);
						dynamicElementsCreatedTotal++;
					}
				}

			}
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
