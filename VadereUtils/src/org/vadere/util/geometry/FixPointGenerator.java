package org.vadere.util.geometry;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.triangulation.IPointConstructor;

import java.awt.geom.PathIterator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Benedikt Zoennchen
 * @param <P>
 */
public class FixPointGenerator<P extends IPoint> {

	public Collection<P> generate(final VShape shape, final Function<IPoint, Double> segmentLenFunc, final IPointConstructor<P> pointConstructor) {
		Set<P> points = new HashSet<>();
		PathIterator path = shape.getPathIterator(null);
		double[] tempCoords = new double[6];
		double[] coordinates = new double[6];
		path.currentSegment(tempCoords);

		while (!path.isDone()) {
			path.next();
			path.currentSegment(coordinates);
			if (coordinates[0] == tempCoords[0] && coordinates[1] == tempCoords[1]) {
				break;
			}

			SplitLineIterator<P> iterator = new SplitLineIterator<>(new VLine(coordinates[0], coordinates[1], tempCoords[0], tempCoords[1]), segmentLenFunc, pointConstructor);

			while (iterator.hasNext()) {
				points.add(iterator.next());
			}

			path.currentSegment(tempCoords);
		}

		return points;
	}

}
