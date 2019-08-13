package org.vadere.simulator.control.util;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class SpawnArray {
	private static Logger logger = Logger.getLogger(SpawnArray.class);
	private static double SPAWN_BUFFER = 0.001;

	protected final VRectangle spawnElementBound;
	protected final VRectangle bound;
	// number of spawn elements in x and y Dimension.
	protected int xDim;
	protected int yDim;

	protected VPoint firstSpawnPoint;
	protected double eX, eY;

	// map valid boundGrid coordinates to #allowedSpawnPoints ArrayList index.
	protected HashMap<Integer, Integer> validSpawnPointMapInBoundShape;
	protected final ArrayList<VPoint> allowedSpawnPoints;
	protected Function<VPoint, VShape> shapeProducer;
	protected SpawnOverlapCheck testFreeSpace;

	protected int nextSpawnPoint;

	public SpawnArray(final VShape boundShape,
					  final VRectangle spawnElementBound,
					  Function<VPoint, VShape> shapeProducer,
					  SpawnOverlapCheck testFreeSpace) {
		this.bound = new VRectangle(boundShape.getBounds2D());
		this.spawnElementBound = spawnElementBound;
		this.shapeProducer = shapeProducer;
		this.testFreeSpace = testFreeSpace;

		xDim = (int) (bound.width / spawnElementBound.width);
		yDim = (int) (bound.height / spawnElementBound.height);

		if (xDim * yDim <= 0) {
			xDim = (xDim == 0) ? 1 : xDim;
			yDim = (yDim == 0) ? 1 : yDim;

			allowedSpawnPoints = new ArrayList<>(xDim * yDim);
			//offset left upper corner to center point.
			eX = (xDim == 1) ? bound.getCenterX() : spawnElementBound.x + spawnElementBound.width / 2 + SPAWN_BUFFER;
			eY = (yDim == 1) ? bound.getCenterY() : spawnElementBound.y + spawnElementBound.height / 2 + SPAWN_BUFFER;
			logger.info(String.format(
					"Dimension of Source is to small for at least one dimension to contain designated spawnElement with Bound (%.2f x %.2f) Set to (%d x %d)",
					spawnElementBound.width, spawnElementBound.height, xDim, yDim));

		} else {
			allowedSpawnPoints = new ArrayList<>(xDim * yDim);
			//offset left upper corner to center point.
			eX = spawnElementBound.x + spawnElementBound.width / 2 + SPAWN_BUFFER;
			eY = spawnElementBound.y + spawnElementBound.height / 2 + SPAWN_BUFFER;
		}

		firstSpawnPoint = new VPoint(bound.x + eX, bound.y + eY);
		validSpawnPointMapInBoundShape = new HashMap<>();
		int validIndex = 0;

		for (int i = 0; i < (xDim * yDim); i++) {
			VPoint candidatePoint = firstSpawnPoint.add(new VPoint(2 * eX * (i % xDim), 2 * eY * (i / xDim)));
			VShape candidateShape = shapeProducer.apply(candidatePoint);
			if (boundShape.containsShape(candidateShape)) {
				validSpawnPointMapInBoundShape.put(i, validIndex);
				allowedSpawnPoints.add(candidatePoint);
				validIndex++;
			}
		}
		allowedSpawnPoints.trimToSize();
		nextSpawnPoint = 0;
	}

	public List<VPoint> getAllowedSpawnPoints() {
		return allowedSpawnPoints;
	}

	/*public Stream<VPoint> streamAllowedSpawnPoints() {

	}*/
}
