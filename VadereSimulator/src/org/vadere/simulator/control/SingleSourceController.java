package org.vadere.simulator.control;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VShape;

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

	public SingleSourceController(Topography scenario, Source source,
								  DynamicElementFactory dynamicElementFactory,
								  AttributesDynamicElement attributesDynamicElement,
								  Random random) {
		super(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
		this.dynamicElementFactory = dynamicElementFactory;
	}

	@Override
	public void update(double simTimeInSec) {
		if (!isSourceFinished(simTimeInSec)) {
			if (simTimeInSec >= timeOfNextEvent || numberToSpawn > 0) {
				determineNumberOfSpawnsAndNextEvent(simTimeInSec);
				List<VPoint> spawnPoints = new LinkedList<>();

				if (sourceAttributes.isSpawnAtRandomPositions()) {

					if (sourceAttributes.isUseFreeSpaceOnly()) {
						//spawnPoints = spawnArray.getNextFreeRandomSpawnPoints(numberToSpawn, random, getDynElementsAtSource());
						spawnPoints = getRealRandomPositions(
								numberToSpawn,
								random,
								getDynElementsAtSource().stream()
										.map(element -> element.getPosition())
										.map(position -> dynamicElementFactory.getDynamicElementRequiredPlace(position))
										.collect(Collectors.toList())
						);

						numberToSpawn -= spawnPoints.size();
						assert (numberToSpawn >= 0);
					} else {
						throw new IllegalArgumentException("use random position without free space only makes no sense.");
						/*spawnPoints = spawnArray.getNextRandomSpawnPoints(numberToSpawn, random, getDynElementsAtSource());
						numberToSpawn -= spawnPoints.size();
						assert (numberToSpawn >= 0);*/
					}

				} else {

					if (sourceAttributes.isUseFreeSpaceOnly()) {
						spawnPoints = spawnArray.getNextFreeSpawnPoints(numberToSpawn, getDynElementsAtSource());
						numberToSpawn -= spawnPoints.size();
						assert (numberToSpawn >= 0);
					} else {
						spawnPoints = spawnArray.getNextSpawnPoints(numberToSpawn, getDynElementsAtSource());
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
			Optional<VPoint> optRandomPosition = getNextRandomPosition(random, blockPedestrianShapes, NUMBER_OF_REPOSITION_TRIES);

			if (optRandomPosition.isPresent()) {
				VPoint randomPosition = optRandomPosition.get();
				blockPedestrianShapes.add(dynamicElementFactory.getDynamicElementRequiredPlace(randomPosition));
				randomPositions.add(randomPosition);
			}
		}

		return randomPositions;
	}

	private Optional<VPoint> getNextRandomPosition(@NotNull final Random random, @NotNull final List<VShape> blockPedestrianShapes, final int tries) {
		Rectangle2D rec = source.getShape().getBounds2D();

		for(int i = 0; i < tries; i++) {
			VPoint randomPoint = new VPoint(rec.getMinX() + random.nextDouble() * rec.getWidth(), rec.getMinY() + random.nextDouble() * rec.getHeight());
			VShape freeSpaceRequired = dynamicElementFactory.getDynamicElementRequiredPlace(randomPoint);

			// no intersection with other free spaces (obstacles & other pedestrians)
			if(blockPedestrianShapes.stream().noneMatch(shape -> shape.intersects(freeSpaceRequired))
					&& 	this.getTopography().getObstacles().stream().noneMatch(obs -> obs.getShape().intersects(freeSpaceRequired))) {
				return Optional.of(randomPoint);
			}
		}

		return Optional.empty();
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
