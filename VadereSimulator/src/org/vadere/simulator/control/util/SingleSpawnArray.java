package org.vadere.simulator.control.util;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

public class SingleSpawnArray extends SpawnArray {

	private final boolean shuffled;

	public SingleSpawnArray(VShape boundShape,
							VRectangle spawnElementBound,
							Function<VPoint, VShape> shapeProducer,
							SpawnOverlapCheck testFreeSpace,
							AttributesSpawner spawnerAttributes) {
		super(boundShape, spawnElementBound, shapeProducer, testFreeSpace, spawnerAttributes);
		this.shuffled = false;
	}

	public void shuffleSpawnPoints(final Random random){
		if(!shuffled) {
			Collections.shuffle(allowedSpawnPoints, random); // shuffle list
		}
	}

	public Optional<VPoint> getNextPosition(@NotNull final List<VShape> blockPedestrianShapes) {
		for (VPoint spawnPoint : allowedSpawnPoints) {
			VShape freeSpaceRequired = shapeProducer.apply(spawnPoint);
			if (testFreeSpace.checkFreeSpace(freeSpaceRequired, blockPedestrianShapes)) {
				return Optional.of(spawnPoint);
			}
		}
		return Optional.empty();
	}
}
