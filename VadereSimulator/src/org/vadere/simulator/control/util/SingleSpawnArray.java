package org.vadere.simulator.control.util;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SingleSpawnArray extends SpawnArray {

	public SingleSpawnArray(VShape boundShape,
							VRectangle spawnElementBound,
							Function<VPoint, VShape> shapeProducer,
							SpawnOverlapCheck testFreeSpace) {
		super(boundShape, spawnElementBound, shapeProducer, testFreeSpace);
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
