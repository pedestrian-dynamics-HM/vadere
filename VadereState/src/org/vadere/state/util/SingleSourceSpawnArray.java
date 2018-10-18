package org.vadere.state.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.List;
import java.util.function.Function;

/**
 * <h1>Single Pedestrians</h1>
 *
 * The single spawn algorithm divides the source in a grid based on the width of the pedestrians.
 * This grid is used to place newly spawn pedestrians. These points are called allowedSpawnPoints and
 * are saved as an 1D-array. Based on the Source Attribute values one of the four functions will
 * be used to select the next allowedSpawnPoints.
 *
 * use the next free spawn point in order (0..n) to place the next pedestrian. This function will
 * try to place up to maxPoints pedestrian an will wrap around to spawnPoint 0 if needed. Also this
 * function will allow overlapping pedestrians a complete overlap is not allowed due to numerical
 * problems in OE-solvers.
 */
public class SingleSourceSpawnArray extends  AbstractSpawnArray{

	private static Logger logger = LogManager.getLogger(SingleSourceSpawnArray.class);
	public static final double D = 0.01;

	public SingleSourceSpawnArray(final VShape boundShape, final VRectangle spawnElementBound, Function<VPoint, VShape> shapeProducer) {
		super(boundShape, spawnElementBound, shapeProducer);
	}


}

