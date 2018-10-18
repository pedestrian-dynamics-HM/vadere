package org.vadere.simulator.control;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.SpawnArray;
import org.vadere.util.geometry.PointPositioned;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class SingleSourceController extends SourceController {

	private int numberToSpawn;
	private DynamicElementFactory dynamicElementFactory;
	private static final int NUMBER_OF_REPOSITION_TRIES = 10;
	private static final int NUMBER_OF_POINT_SEARCH = 1_000; // todo based on shape and position of source

	private SpawnArray spawnArray;
	public SingleSourceController(Topography scenario, Source source,
								  DynamicElementFactory dynamicElementFactory,
								  AttributesDynamicElement attributesDynamicElement,
								  Random random) {
		super(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
		VRectangle elementBound = new VRectangle(dynamicElementFactory.getDynamicElementRequiredPlace(new VPoint(0,0)).getBounds2D());
		this.spawnArray = new SpawnArray(source.getShape(),
				new VRectangle(0, 0,elementBound.getWidth(), elementBound.getHeight()),
				dynamicElementFactory::getDynamicElementRequiredPlace);
	}

	@Override
	public void update(double simTimeInSec) {
		if (!isSourceFinished(simTimeInSec)) {
			if (simTimeInSec >= timeOfNextEvent || numberToSpawn > 0) {
				determineNumberOfSpawnsAndNextEvent(simTimeInSec);
				List<VPoint> spawnPoints = new LinkedList<>();

				if (sourceAttributes.isSpawnAtRandomPositions()) {

					if (sourceAttributes.isUseFreeSpaceOnly()) {
						spawnPoints = getRealRandomPositions(
								numberToSpawn,
								random,
								getDynElementsAtSource().stream()
										.map(PointPositioned::getPosition)
										.map(position -> dynamicElementFactory.getDynamicElementRequiredPlace(position))
										.collect(Collectors.toList())
						);

						numberToSpawn -= spawnPoints.size();
						assert (numberToSpawn >= 0);
					} else {
						throw new IllegalArgumentException("use random position without free space only makes no sense.");
					}

				} else {

					if (sourceAttributes.isUseFreeSpaceOnly()) {
						spawnPoints = getRealPositions(
								numberToSpawn,
								getDynElementsAtSource().stream()
										.map(PointPositioned::getPosition)
										.map(position -> dynamicElementFactory.getDynamicElementRequiredPlace(position))
										.collect(Collectors.toList())
						);
						numberToSpawn -= spawnPoints.size();
						assert (numberToSpawn >= 0);
					} else {
						spawnPoints = getRealPositions(
								numberToSpawn,
								new ArrayList<>()
						);
						numberToSpawn -= spawnPoints.size();
						assert (numberToSpawn >= 0);
					}

				}

				for (VPoint spawnPoint : spawnPoints) {
					if (!isMaximumNumberOfSpawnedElementsReached()) {
						addNewAgentToScenario(spawnPoint);
						dynamicElementsCreatedTotal++;
					}
				}
			}
		}
	}

	private List<VPoint> getRealPositions(final int numberToSpawn, @NotNull final List<VShape> blockPedestrianShapes){
		List<VPoint> positions = new ArrayList<>(numberToSpawn);

		for(int i = 0; i < numberToSpawn; i++) {
			Optional<VPoint> optPosition = getNextPosition(blockPedestrianShapes, spawnArray.getAllowedSpawnPoints());

			if (optPosition.isPresent()) {
				VPoint position = optPosition.get();
				blockPedestrianShapes.add(dynamicElementFactory.getDynamicElementRequiredPlace(position));
				positions.add(position);
			}
		}
		return positions;
	}


	private Optional<VPoint> getNextPosition(List<VShape> blockPedestrianShapes, List<VPoint> spawnPoints) {

		for (VPoint spawnPoint  : spawnPoints){
			VShape freeSpaceRequired = dynamicElementFactory.getDynamicElementRequiredPlace(spawnPoint);
			if (testFreeSpace(freeSpaceRequired, blockPedestrianShapes)){
				return Optional.of(spawnPoint);
			}
		}
		return Optional.empty();
	}


	/**
	 * Computes numberToSpawn or less random positions based on the blockPedestrianShapes which contains the shapes representing the required space of each pedestrian.
	 * For each required position the algorithms tries {@link SingleSourceController#NUMBER_OF_REPOSITION_TRIES} times to get a feasible free position.
	 *
	 * @param numberToSpawn         number of required spawn positions
	 * @param random                random generator
	 * @param blockPedestrianShapes  the required space of other pedestrians
	 * @return numberToSpawn or less random feasible positions
	 */
	private List<VPoint> getRealRandomPositions(final int numberToSpawn, @NotNull final Random random, @NotNull final List<VShape> blockPedestrianShapes) {
		List<VPoint> randomPositions = new ArrayList<>(numberToSpawn);

		for(int i = 0; i < numberToSpawn; i++) {
			Optional<VPoint> optRandomPosition = getNextRandomPosition(random, blockPedestrianShapes, NUMBER_OF_POINT_SEARCH, NUMBER_OF_REPOSITION_TRIES);

			if (optRandomPosition.isPresent()) {
				VPoint randomPosition = optRandomPosition.get();
				blockPedestrianShapes.add(dynamicElementFactory.getDynamicElementRequiredPlace(randomPosition));
				randomPositions.add(randomPosition);
			}
		}

		return randomPositions;
	}


	private Optional<VPoint> getNextRandomPosition(@NotNull final Random random, @NotNull final List<VShape> blockPedestrianShapes,
												   final int tries_find_valid_point, final int tries_reposition) {
		Rectangle2D rec = source.getShape().getBounds2D();

		for(int i = 0; i < tries_reposition; i++) {
			VShape freeSpaceRequired = null;
			VPoint randomPoint = null;
			boolean pointFound = false;
			// find point in source boundary
			int j = 0;
			while(j < tries_find_valid_point && !pointFound){
				randomPoint = new VPoint(rec.getMinX() + random.nextDouble() * rec.getWidth(), rec.getMinY() + random.nextDouble() * rec.getHeight());
				freeSpaceRequired = dynamicElementFactory.getDynamicElementRequiredPlace(randomPoint);
				pointFound = source.getShape().containsShape(freeSpaceRequired);
				j++;
			}

			// no intersection with other free spaces (obstacles & other pedestrians)
			if(testFreeSpace(freeSpaceRequired, blockPedestrianShapes)) {
				return Optional.of(randomPoint);
			}
		}

		return Optional.empty();
	}

	private boolean testFreeSpace(final VShape freeSpace, final List<VShape> blockPedestrianShapes){
		return blockPedestrianShapes.stream().noneMatch(shape -> shape.intersects(freeSpace)) &&
				this.getTopography().getObstacles().stream()
						.map(Obstacle::getShape).noneMatch(shape -> shape.intersects(freeSpace));
	}

	@Override
	protected boolean isQueueEmpty() {
		return numberToSpawn == 0;
	}

	@Override
	protected void determineNumberOfSpawnsAndNextEvent(double simTimeInSec) {
		while (timeOfNextEvent <= simTimeInSec) {
			numberToSpawn += sourceAttributes.getSpawnNumber();
			createNextEvent();
		}
	}

}
