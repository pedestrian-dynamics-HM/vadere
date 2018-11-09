package org.vadere.meshing;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.geom.Line2D;
import java.util.Iterator;

public class ConstantLineIterator implements Iterator<IPoint> {

	private final Line2D.Double line;
	private final double delta;
	private VPoint startPoint;
	private VPoint currentPoint;
	private VPoint endPoint;
	private VPoint deltaPoint;
	private double slope;
	private double lineLength;

	private double dx;
	private double dy;
	private int counter;
	private int numberOfSegments;

	public ConstantLineIterator(final Line2D.Double line, final double delta) {
		this.line = line;

		if(line.getX1() < line.getX2()) {
			startPoint = new VPoint(line.getX1(), line.getY1());
			endPoint = new VPoint(line.getX2(), line.getY2());
		}
		else if(line.getX1() == line.getX2()) {
			if(line.getY1() < line.getY2()) {
				startPoint = new VPoint(line.getX1(), line.getY1());
				endPoint = new VPoint(line.getX2(), line.getY2());
			}
			else if(line.getY1() > line.getY2()) {
				startPoint = new VPoint(line.getX2(), line.getY2());
				endPoint = new VPoint(line.getX1(), line.getY1());
			}
			else {
				throw new IllegalArgumentException(line + " is not a feasible line.");
			}
		}
		else {
			startPoint = new VPoint(line.getX2(), line.getY2());
			endPoint = new VPoint(line.getX1(), line.getY1());
		}

		lineLength = startPoint.distance(endPoint);

		numberOfSegments = (int)Math.floor(lineLength / delta) - 3;
		this.delta = lineLength / numberOfSegments;

		if(line.getX1() == line.getX2()) {
			dx = 0;
			dy = this.delta;
			slope = 0;
		}

		if(line.getY1() == line.getY2()) {
			dx = this.delta;
			dy = 0;
			slope = 0;
		}

		if(line.getX1() != line.getX2() && line.getY1() != line.getY2()) {
			double len = startPoint.distance(endPoint);
			slope = new VLine(startPoint, endPoint).slope();
			dx = Math.sqrt((this.delta * this.delta) / (1 + slope*slope));
			dy = dx * slope;

		}

		deltaPoint = new VPoint(dx, dy);
		currentPoint = null;
	}

	@Override
	public boolean hasNext() {
		return currentPoint == null || !currentPoint.equals(endPoint);
	}

	@Override
	public IPoint next() {
		// first point
		if(currentPoint == null) {
			counter++;
			currentPoint = startPoint;
		}
		else if(counter < numberOfSegments) {
			counter++;
			currentPoint = currentPoint.add(deltaPoint);
		} // last point
		else {
			currentPoint = endPoint;
		}

		if(slope != 0) {
			// this is more accurate for slope != 0.
			return new VPoint(currentPoint.getX(), startPoint.getY() + slope * (currentPoint.getX() - startPoint.getX()));
		}
		else {
			return currentPoint;
		}
	}
}
