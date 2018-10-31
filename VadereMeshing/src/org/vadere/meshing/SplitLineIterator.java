package org.vadere.meshing;

import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * @author Benedikt Zoennchen
 */
public class SplitLineIterator<P extends IPoint> implements Iterator<P>{

	private final Function<IPoint, Double> segmentLenFunc;
	private final List<P> points;
	private final IPointConstructor<P> pointConstructor;
	private final Iterator<P> iterator;

	public SplitLineIterator(final VLine line, final Function<IPoint, Double> segmentLenFunc, final IPointConstructor<P> pointConstructor) {
		this.segmentLenFunc = segmentLenFunc;
		this.pointConstructor = pointConstructor;
		this.points = new ArrayList<>();
		this.points.add(pointConstructor.create(line.getX1(), line.getY1()));
		this.points.add(pointConstructor.create(line.getX2(), line.getY2()));

		splitLine(line);
		iterator = points.iterator();
	}

	public void splitLine(final VLine line) {
		VPoint midPoint = line.midPoint();
		if(segmentLenFunc.apply(midPoint) < line.length()) {
			points.add(pointConstructor.create(midPoint.getX(), midPoint.getY()));
			VPoint start = new VPoint(line.getX1(), line.getY1());
			VPoint end = new VPoint(line.getX2(), line.getY2());

			splitLine(new VLine(start, midPoint));
			splitLine(new VLine(midPoint, end));
		}
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public P next() {
		return iterator.next();
	}
}
