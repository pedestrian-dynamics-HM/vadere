package org.vadere.simulator.control.scenarioelements;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.control.util.GroupSpawnArray;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.simulator.models.groups.GroupModel;
import org.vadere.simulator.models.groups.GroupSizeDeterminator;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.PointPositioned;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.stream.Collectors;

public class GroupSourceController extends SourceController {

	private static final int NUMBER_OF_REPOSITION_TRIES = 20;


	private final GroupModel groupModel;
	private final LinkedList<Integer> groupsToSpawn;
	private final GroupSpawnArray spawnArray;

	public GroupSourceController(Topography scenario, Source source,
								 DynamicElementFactory dynamicElementFactory,
								 AttributesDynamicElement attributesDynamicElement,
								 Random random, GroupModel groupModel, GroupSizeDeterminator gsd) {
		super(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
		this.groupModel = groupModel;
		this.groupModel.registerGroupSizeDeterminator(source.getId(), gsd);
		this.groupsToSpawn = new LinkedList<>();

		VRectangle elementBound = new VRectangle(dynamicElementFactory.getDynamicElementRequiredPlace(new VPoint(0, 0)).getBounds2D());

		this.spawnArray = new GroupSpawnArray(source.getShape(),
				new VRectangle(0, 0, elementBound.getWidth(), elementBound.getHeight()),
				dynamicElementFactory::getDynamicElementRequiredPlace,
				this::testFreeSpace,
				source.getAttributes().getSpawnerAttributes());

	}

	@Override
	public void update(double simTimeInSec) {
		if (!spawner.isFinished(simTimeInSec, () -> isQueueEmpty())) {
			if (simTimeInSec >= timeOfNextEvent || !groupsToSpawn.isEmpty()) {
				determineNumberOfSpawnsAndNextEvent(simTimeInSec);

				if (spawnerAttributes.isEventPositionRandom()) {

					if (spawnerAttributes.isEventPositionFreeSpace()) {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = getRealRandomPositions(
									groupSize,
									random,
									getDynElementsAtSource().stream()
											.map(PointPositioned::getPosition)
											.map(dynamicElementFactory::getDynamicElementRequiredPlace)
											.collect(Collectors.toList())
							);
							if (newGroup.size() > 0) {
								// add immediately to Scenario to update DynElementsAtSource
								addElementToScenario(newGroup, simTimeInSec);
								iter.remove();
							} else {
								break; // FIFO Spawn. The rest of the queue at next time step
							}
						}
					} else {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextGroup(
									groupSize,
									random,
									getDynElementsAtSource().stream()
											.map(PointPositioned::getPosition)
											.map(dynamicElementFactory::getDynamicElementRequiredPlace)
											.collect(Collectors.toList())
							);
							if (newGroup.isEmpty())
								throw new RuntimeException("Cannot spawn new Group. Source " + source.getId() + " is set " +
										"to useFreeSpaceOnly == false but no space is left to spawn group without exactly" +
										"overlapping with neighbours which can cause numerical problems. Use useFreeSpaceOnly == true (default)" +
										"to queue groups.");
							addElementToScenario(newGroup, simTimeInSec);
							iter.remove();
						}
					}

				} else {

					if (spawnerAttributes.isEventPositionFreeSpace()) {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextFreeGroup(
									groupSize,
									getDynElementsAtSource().stream()
											.map(PointPositioned::getPosition)
											.map(dynamicElementFactory::getDynamicElementRequiredPlace)
											.collect(Collectors.toList())
							);
							if (newGroup != null && !newGroup.isEmpty()) {
								// add immediately to Scenario to update DynElementsAtSource
								addElementToScenario(newGroup, simTimeInSec);
								iter.remove();
							} else {
								break; // FIFO Spawn. The rest of the queue at next time step
							}
						}
					} else {
						Iterator<Integer> iter = groupsToSpawn.iterator();
						while (iter.hasNext()) {
							int groupSize = iter.next();
							List<VPoint> newGroup = spawnArray.getNextGroup(
									groupSize,
									getDynElementsAtSource().stream()
											.map(PointPositioned::getPosition)
											.map(dynamicElementFactory::getDynamicElementRequiredPlace)
											.collect(Collectors.toList())
							);
							if (newGroup == null || newGroup.isEmpty())
								throw new RuntimeException("Cannot spawn new Group. Source " + source.getId() + " is set " +
										"to useFreeSpaceOnly == false but no space is left to spawn group without exactly" +
										"overlapping with neighbours which can cause numerical problems. Use useFreeSpaceOnly == true (default)" +
										"to queue groups.");
							addElementToScenario(newGroup, simTimeInSec);
							iter.remove();
						}
					}

				}


			}
		}
	}


	/**
	 * Computes random positions for ONE group  based on the blockPedestrianShapes which contains
	 * the shapes representing the required space for the specified group size. For each required
	 * position the algorithms tries {@link GroupSourceController#NUMBER_OF_REPOSITION_TRIES} times
	 * to get a feasible free position for this group.
	 *
	 * @param groupSize             size of group to spawn at a random positions
	 * @param random                random generator
	 * @param blockPedestrianShapes the required space of other pedestrians
	 * @return list of Points representing the group members or an empty list if group cannot be
	 * placed after {@link GroupSourceController#NUMBER_OF_REPOSITION_TRIES} of tries.
	 */
	private List<VPoint> getRealRandomPositions(final int groupSize, @NotNull final Random random, @NotNull final List<VShape> blockPedestrianShapes) {
		List<VPoint> randomPositions;

		List<VPoint> defaultPoints = spawnArray.getDefaultGroup(groupSize);
		for (int i = 0; i < NUMBER_OF_REPOSITION_TRIES; i++) {
			randomPositions = moveRandomInSourceBound(defaultPoints, random);
			boolean groupValid = randomPositions.stream()
					.map(dynamicElementFactory::getDynamicElementRequiredPlace)
					.allMatch(candidateShape ->
							source.getShape().containsShape(candidateShape) &&
									testFreeSpace(candidateShape, blockPedestrianShapes));
			if (groupValid) {
				return randomPositions;
			}
		}

		return new ArrayList<>();
	}

	/**
	 * @param points default points of group members. First allowed position if the spawn would be
	 *               based on the spawn grid.
	 * @param random random object
	 * @return transformed set of points based on a random translation and rotation within the bound
	 * of the source
	 */
	private List<VPoint> moveRandomInSourceBound(List<VPoint> points, @NotNull final Random random) {
		Rectangle2D bound = source.getShape().getBounds2D();
		double angle = random.nextDouble() * 2 * Math.PI;
		VPoint p0 = points.get(0);
		double dxBound = (bound.getX() - p0.getX());
		double dyBound = (bound.getY() - p0.getY());
		double dxRnd = random.nextDouble() * (bound.getMaxX() - bound.getX());
		double dyRnd = random.nextDouble() * (bound.getMaxY() - bound.getY());
		AffineTransform at0 = new AffineTransform();
		at0.setToTranslation(dxBound, dyBound);
		AffineTransform at1 = new AffineTransform();
		at1.setToRotation(angle, bound.getX(), bound.getY());
		AffineTransform at2 = new AffineTransform();
		at2.setToTranslation(dxRnd, dyRnd);

		List<VPoint> ret = new ArrayList<>();

		points.stream().map(VPoint::asPoint2D).forEach(p -> {
					at0.transform(p, p);
					at1.transform(p, p);
					at2.transform(p, p);
					ret.add(new VPoint(p));
				}
		);
		return ret;
	}

	private void addElementToScenario(List<VPoint> group, double simTimeInSec) {
		if (!group.isEmpty() && !spawner.isMaximumNumberOfSpawnedElementsReached()) {
			addNewAgentToScenario(group, simTimeInSec);
			spawner.incrementElementsCreatedTotal(group.size());
		}
	}

	private void getNewGroupSizeFromModel() {
		for (int i = 0; i < spawnerAttributes.getEventElementCount(); i++) {
			int groupSize = groupModel.nextGroupForSource(this.source.getId());
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
