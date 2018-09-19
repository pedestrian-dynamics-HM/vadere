package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;


public class SimpleDistanceEdgeLenFunction implements IEdgeLengthFunction {
	private final VRectangle boundingBox;
	private final IDistanceFunction distanceFunc;

	public SimpleDistanceEdgeLenFunction(final VRectangle boundingBox,
	                               final IDistanceFunction distanceFunc) {
		this.boundingBox = boundingBox;
		this.distanceFunc = distanceFunc;
	}

	@Override
	public Double apply(IPoint iPoint) {
		return 0.00005 - 0.000001 * distanceFunc.apply(iPoint);
	}
}