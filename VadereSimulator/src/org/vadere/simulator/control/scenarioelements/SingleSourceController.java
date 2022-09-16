package org.vadere.simulator.control.scenarioelements;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.control.util.SingleSpawnArray;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.PointPositioned;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.stream.Collectors;

public class SingleSourceController extends SourceController {

	private static final Logger logger = Logger.getLogger(SingleSourceController.class);

	private int numberToSpawn;
	private static final int NUMBER_OF_REPOSITION_TRIES = 10;
	private static final int NUMBER_OF_POINT_SEARCH = 1_500; // todo based on shape and position of source

	private final SingleSpawnArray spawnArray;

	public SingleSourceController(Topography scenario, Source source,
								  DynamicElementFactory dynamicElementFactory,
								  AttributesDynamicElement attributesDynamicElement,
								  Random random) {

		super(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
		VRectangle elementBound = new VRectangle(dynamicElementFactory.getDynamicElementRequiredPlace(new VPoint(0, 0)).getBounds2D());

		this.spawnArray = new SingleSpawnArray(
				source.getShape(),
				new VRectangle(0, 0, elementBound.getWidth(), elementBound.getHeight()),
				this.dynamicElementFactory::getDynamicElementRequiredPlace,
				this::testFreeSpace,
				sourceAttributes.getSpawnerAttributes());
	}

	@Override
	public void update(double simTimeInSec) {
		if (!this.spawner.isFinished(simTimeInSec, () -> isQueueEmpty())) {
			determineNumberOfSpawnsAndNextEvent(simTimeInSec);

			if (numberToSpawn > 0) {
				List<VPoint> spawnPoints;

				if (spawnerAttributes.isEventPositionRandom()) {

					if (!spawnerAttributes.isEventPositionGridCA()) {

						spawnPoints = getRealRandomPositions(
								numberToSpawn,
								random,
								getDynElementsAtSource().stream()
										.map(PointPositioned::getPosition)
										.map(dynamicElementFactory::getDynamicElementRequiredPlace)
										.collect(Collectors.toList())
						);

					}else{
						spawnPoints = getRandomArrayPositions(
								numberToSpawn,
								random,
								getDynElementsAtSource().stream()
										.map(PointPositioned::getPosition)
										.map(dynamicElementFactory::getDynamicElementRequiredPlace)
										.collect(Collectors.toList())
						);

					}


				} else {

					if (spawnerAttributes.isEventPositionFreeSpace()) {
						spawnPoints = getRealPositions(
								numberToSpawn,
								getDynElementsAtSource().stream()
										.map(PointPositioned::getPosition)
										.map(dynamicElementFactory::getDynamicElementRequiredPlace)
										.collect(Collectors.toList())
						);

					} else {
						spawnPoints = getRealPositions(
								numberToSpawn,
								new ArrayList<>()
						);
					}

				}

				// Report nr. of agents that could not be spawned -- it is up to SpawnDistribution if it
				// wants to try to spawn the agents in the next update.
				int remainingAgents = numberToSpawn - spawnPoints.size();
				assert (remainingAgents >= 0);
				this.spawner.setRemainingSpawnAgents(remainingAgents);

				for (VPoint spawnPoint : spawnPoints) {
					if (!spawner.isMaximumNumberOfSpawnedElementsReached()) {
						addNewAgentToScenario(spawnPoint, simTimeInSec);
						spawner.incrementElementsCreatedTotal(1);
					}
				}
			}
		}
	}


	/**
	 * Computes numberToSpawn random positions within the source on a SpawnArray.
	 * @param numberToSpawn number of required spawn positions
	 * @param random random generator
	 * @param blockPedestrianShapes the required space of other pedestrians
	 * @return numberToSpawn or less random feasible positions
	 */
	private List<VPoint> getRandomArrayPositions(final int numberToSpawn, @NotNull final Random random, @NotNull final List<VShape> blockPedestrianShapes) {
		spawnArray.shuffleSpawnPoints(random); // shuffle list of possible positions

		return getRealPositions(numberToSpawn, blockPedestrianShapes);
	}

	private List<VPoint> getRealPositions(final int numberToSpawn, @NotNull final List<VShape> blockPedestrianShapes) {
		List<VPoint> positions = new LinkedList<>();

		for (int i = 0; i < numberToSpawn; i++) {
			Optional<VPoint> optPosition = spawnArray.getNextPosition(blockPedestrianShapes);

			if (optPosition.isPresent()) {
				VPoint position = optPosition.get();
				blockPedestrianShapes.add(dynamicElementFactory.getDynamicElementRequiredPlace(position));
				positions.add(position);
			}


		}
		return positions;
	}

	/**
	 * Computes numberToSpawn or less random positions based on the blockPedestrianShapes which
	 * contains the shapes representing the required space of each pedestrian. For each required
	 * position the algorithms tries {@link SingleSourceController#NUMBER_OF_REPOSITION_TRIES} times
	 * to get a feasible free position.
	 *
	 * @param numberToSpawn         number of required spawn positions
	 * @param random                random generator
	 * @param blockPedestrianShapes the required space of other pedestrians
	 * @return numberToSpawn or less random feasible positions
	 */
	private List<VPoint> getRealRandomPositions(final int numberToSpawn, @NotNull final Random random, @NotNull final List<VShape> blockPedestrianShapes) {

		List<VPoint> randomPositions = new LinkedList<VPoint>();

		int setNumberAgents = 0;

		for (int i = 0; i < numberToSpawn; i++) {
			Optional<VPoint> optRandomPosition = getNextRandomPosition(
					random,
					blockPedestrianShapes,
					NUMBER_OF_POINT_SEARCH,
					NUMBER_OF_REPOSITION_TRIES);

			if (optRandomPosition.isPresent()) {
				VPoint randomPosition = optRandomPosition.get();
				blockPedestrianShapes.add(dynamicElementFactory.getDynamicElementRequiredPlace(randomPosition));
				randomPositions.add(randomPosition);
				setNumberAgents = i+1;
			}else{
				break;
            }
		}

		if(setNumberAgents != numberToSpawn){
			logger.debug("Could only set " +
					setNumberAgents + "/" + setNumberAgents +" agents. " +
					"Either the source is too small or spawn number too high.");
		}

		return randomPositions;
	}


	private Optional<VPoint> getNextRandomPosition(@NotNull final Random random, @NotNull final List<VShape> blockPedestrianShapes,
												   final int triesFindValidPoint, final int triesReposition) {
		Rectangle2D rec = source.getShape().getBounds2D();

		for (int i = 0; i < triesReposition; i++) {
			VShape freeSpaceRequired = null;
			VPoint randomPoint = null;
			boolean pointFound = false;
			// find point in source boundary
			int j = 0;
			while (j < triesFindValidPoint && !pointFound) {
				randomPoint = new VPoint(rec.getMinX() + random.nextDouble() * rec.getWidth(), rec.getMinY() + random.nextDouble() * rec.getHeight());
				freeSpaceRequired = dynamicElementFactory.getDynamicElementRequiredPlace(randomPoint);
				pointFound = source.getShape().containsShape(freeSpaceRequired);
				j++;
			}

			// no intersection with other free spaces (obstacles & other pedestrians)

			if (!spawnerAttributes.isEventPositionFreeSpace() || testFreeSpace(freeSpaceRequired, blockPedestrianShapes)) {
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

		// The concrete distribution implements how to proceed with agents that could not all be spawned
		// e.g. because the source is too small
		numberToSpawn = this.spawner.getRemainingSpawnAgents();

		while (timeOfNextEvent <= simTimeInSec) {
			numberToSpawn += spawner.getEventElementCount(timeOfNextEvent);
			createNextEvent();
		}
	}

}
