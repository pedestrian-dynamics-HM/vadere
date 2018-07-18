package org.vadere.simulator.control;

import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.LinkedList;
import java.util.Random;

public class SingleSourceController extends SourceController {

	private int numberToSpawn;

	public SingleSourceController(Topography scenario, Source source,
								  DynamicElementFactory dynamicElementFactory,
								  AttributesDynamicElement attributesDynamicElement,
								  Random random) {
		super(scenario, source, dynamicElementFactory, attributesDynamicElement, random);
	}

	@Override
	public void update(double simTimeInSec) {
		if (!isSourceFinished(simTimeInSec)) {
			if (simTimeInSec >= timeOfNextEvent || numberToSpawn > 0) {
				determineNumberOfSpawnsAndNextEvent(simTimeInSec);
				LinkedList<VPoint> spawnPoints = new LinkedList<>();

				if (sourceAttributes.isSpawnAtRandomPositions()) {

					if (sourceAttributes.isUseFreeSpaceOnly()) {
						spawnPoints = spawnArray.getNextFreeRandomSpawnPoints(numberToSpawn, random, getDynElementsAtSource());
						numberToSpawn -= spawnPoints.size();
						assert (numberToSpawn >= 0);
					} else {
						spawnPoints = spawnArray.getNextRandomSpawnPoints(numberToSpawn, random, getDynElementsAtSource());
						numberToSpawn -= spawnPoints.size();
						assert (numberToSpawn >= 0);
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
