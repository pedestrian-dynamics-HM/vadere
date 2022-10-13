package org.vadere.simulator.control.util;

import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;


public class SpawnArray {
	private static final Logger logger = Logger.getLogger(SpawnArray.class);
	private static double SPAWN_BUFFER = 0.001;
	private final double grid_resolution_ca = 0.4;

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
					  SpawnOverlapCheck testFreeSpace,
					  final AttributesSpawner spawnerAttributes) {
		this.bound = new VRectangle(boundShape.getBounds2D());
		this.spawnElementBound = spawnElementBound;
		this.shapeProducer = shapeProducer;
		this.testFreeSpace = testFreeSpace;

		if(spawnerAttributes.isEventPositionGridCA()){
			SPAWN_BUFFER = 0;
		}

		/* cellular automaton */
		double offset_x_low = 0;
		double offset_y_low = 0;
		double offset_x_high = 0;
		double offset_y_high = 0;

		if(spawnerAttributes.isEventPositionGridCA()){
			offset_x_low = calculateOffsetLow(bound.x);
			offset_x_high = calculateOffsetHigh(bound.x+bound.width);
			offset_y_low = calculateOffsetLow(bound.y);
			offset_y_high = calculateOffsetHigh(bound.y+bound.height);
		}

		xDim = (int) ((bound.width - offset_x_low - offset_x_high)/ spawnElementBound.width);
		yDim = (int) ((bound.height- offset_y_low - offset_y_high ) / spawnElementBound.height);

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

		firstSpawnPoint = new VPoint(bound.x + eX + offset_x_low, bound.y + eY + offset_y_low);
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

	private double roundTo3DecimalPlaces(double toRound){
		return Math.round(toRound*1000.0)/1000.0;
	}

	private double calculateOffsetLow (double bound_low){
		double tmp_x = roundTo3DecimalPlaces((bound_low/ grid_resolution_ca) % 1);
		double tmp_x_offset = roundTo3DecimalPlaces((1 - tmp_x) * grid_resolution_ca);
		return tmp_x > 0 && tmp_x < 1? tmp_x_offset : 0.0 ;
	}
	private double calculateOffsetHigh (double bound_high) {

		double tmp_y = roundTo3DecimalPlaces((bound_high / grid_resolution_ca) % 1);
		double tmp_y_offset = roundTo3DecimalPlaces(tmp_y * grid_resolution_ca);
		return tmp_y > 0 && tmp_y < 1 ? tmp_y_offset : 0.0;
	}

}
