package org.vadere.state.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <h1>Single Pedestrians</h1>
 *
 * The single spawn algorithm divides the source in a grid based on the width of the pedestrians.
 * This grid is used to place newly spawn pedestrians. These points are called spawnPoints and
 * are saved as an 1D-array. Based on the Source Attribute values one of the four functions will
 * be used to select the next spawnPoints.
 *
 * use the next free spawn point in order (0..n) to place the next pedestrian. This function will
 * try to place up to maxPoints pedestrian an will wrap around to spawnPoint 0 if needed. Also this
 * function will allow overlapping pedestrians a complete overlap is not allowed due to numerical
 * problems in OE-solvers.
 */
public class SingleSourceSpawnArray {

	private static Logger logger = LogManager.getLogger(SingleSourceSpawnArray.class);
	public static final double D = 0.01;
	private final ArrayList<VPoint> spawnPoints;

	public SingleSourceSpawnArray(final VShape boundShape, final VRectangle spawnElementBound, Function<VPoint, VShape> shapeProducer) {
		VRectangle bound = new VRectangle(boundShape.getBounds2D());
		// number of spawn elements in x and y Dimension.
		int xDim = (int) (bound.width / (spawnElementBound.width + D));
		int yDim = (int) (bound.height / (spawnElementBound.height + D));
//		System.out.printf("SpawnElement: %f | %f %n", spawnElementBound.width, spawnElementBound.height);

		double eX, eY;
		if (xDim * yDim <= 0) {
			xDim = (xDim == 0) ? 1 : xDim;
			yDim = (yDim == 0) ? 1 : yDim;

			spawnPoints = new ArrayList<>(xDim * yDim);
			//offset left upper corner to center point.
			eX = (xDim == 1) ? bound.getCenterX() : spawnElementBound.x + spawnElementBound.width / 2 + D / 2;
			eY = (yDim == 1) ? bound.getCenterY() : spawnElementBound.y + spawnElementBound.height / 2 + D / 2;
			logger.info(String.format(
					"Dimension of Source is to small for at least one dimension to contain designated spawnElement with Bound (%.2f x %.2f) Set to (%d x %d)",
					spawnElementBound.width, spawnElementBound.height, xDim, yDim));

		} else {
			spawnPoints = new ArrayList<>(xDim * yDim);
			//offset left upper corner to center point.
			eX = spawnElementBound.x + spawnElementBound.width / 2 + D/2;
			eY = spawnElementBound.y + spawnElementBound.height / 2 + D/2;
		}

		VPoint firstSpawnPoint = new VPoint(bound.x + eX, bound.y + eY);

		for (int i = 0; i < (xDim * yDim); i++) {
			VPoint candidatePoint = firstSpawnPoint.add(new VPoint(2 * eX * (i % xDim), 2 * eY * (i / xDim)));
			VShape candidateShape = shapeProducer.apply(candidatePoint);
			if (boundShape.containsShape(candidateShape)) {
				spawnPoints.add(candidatePoint);
			}
		}
		spawnPoints.trimToSize();

	}

	/**
	 * @return copy of spawnPoint used for underling source shape.
	 */
	public ArrayList<VPoint> getSpawnPoints() {
		return spawnPoints;
	}
}

