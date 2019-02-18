package org.vadere.util.geometry.shapes;

import java.awt.geom.Line2D;
import java.util.stream.Stream;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.Vector2D;

@SuppressWarnings("serial")
public class VLine extends Line2D.Double {
    private VPoint p1;
    private VPoint p2;

    public VLine(final VPoint p1, final VPoint p2) {
        super(p1.getX(), p1.getY(), p2.getX(), p2.getY());
		/*if(p1.equals(p2)) {
			throw new IllegalArgumentException(p1 + " is equal " + p2);
		}*/
        this.p1 = p1;
        this.p2 = p2;
    }

    public VLine(double x1, double y1, double x2, double y2) {
        this(new VPoint(x1, y1), new VPoint(x2, y2));
    }

    public double ptSegDist(IPoint point) {
        return super.ptSegDist(point.getX(), point.getY());
    }

    public double slope() {
        return (y2 - y1) / (x2 - x1);
    }

    public double distance(IPoint point) {
        return GeometryUtils.closestToSegment(this, point).distance(point);
    }

    public VPoint midPoint() {
        return p1.add(p2).scalarMultiply(0.5);
    }

	public VPoint midPoint(double eps) {
    	assert eps > -0.5 && eps < 0.5;
    	VPoint p3 = p2.subtract(p1);
		return p1.add(p3.scalarMultiply(0.5 + eps));
	}

    @Override
    public int hashCode() {
        // this has to be symmetric
        return super.getP1().hashCode() * getP2().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Line2D.Double) {
            Line2D.Double other = (Line2D.Double)obj;
            return getP1().equals(other.getP1()) && getP2().equals(other.getP2()) || getP1().equals(other.getP2()) && getP2().equals(other.getP1());
        }
        return false;
    }

    public Stream<VPoint> streamPoints() {
        return Stream.of(p1, p2);
    }

    public double length() {
        return getP1().distance(getP2());
    }
	public double distance(VPoint point) {
		return GeometryUtils.closestToSegment(this, point).distance(point);
	}

	public Vector2D asVector(){
		return new Vector2D(x2 - x1, y2 - y1);
	}

	@Override
	public String toString() {
		return p1 + " - " + p2;
	}
}
