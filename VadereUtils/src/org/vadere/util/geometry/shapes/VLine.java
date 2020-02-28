package org.vadere.util.geometry.shapes;

import com.github.davidmoten.rtree.geometry.Circle;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Line;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.geometry.internal.GeometryUtil;
import com.github.davidmoten.rtree.internal.RectangleUtil;

import java.awt.geom.Line2D;
import java.util.stream.Stream;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.Vector2D;


@SuppressWarnings("serial")
public class VLine extends Line2D.Double implements Geometry {
    private VPoint p1;
    private VPoint p2;
    private double length;
    private double lengthSq;

    public VLine(final VPoint p1, final VPoint p2) {
        super(p1.getX(), p1.getY(), p2.getX(), p2.getY());
		/*if(p1.equals(p2)) {
			throw new IllegalArgumentException(p1 + " is equal " + p2);
		}*/
        this.p1 = p1;
        this.p2 = p2;
        this.length = -1;
        this.lengthSq = -1;
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

	public VPoint getVPoint1() {
    	return new VPoint(getX1(), getY1());
	}

	public VPoint getVPoint2() {
    	return new VPoint(getX2(), getY2());
	}

   /* @Override
    public int hashCode() {
        // this has to be symmetric
        return super.getP1().hashCode() * getP2().hashCode();
    }*/

    /*@Override
    public boolean equals(Object obj) {
        if(obj instanceof Line2D.Double) {
            Line2D.Double other = (Line2D.Double)obj;
            return getP1().equals(other.getP1()) && getP2().equals(other.getP2()) || getP1().equals(other.getP2()) && getP2().equals(other.getP1());
        }
        return false;
    }*/

    public Stream<VPoint> streamPoints() {
        return Stream.of(p1, p2);
    }

    public double length() {
        if(length == -1) {
        	length = getP1().distance(getP2());
        }
    	return length;
    }

    public double lengthSq() {
    	if(lengthSq == -1) {
		    lengthSq = getP1().distanceSq(getP2());
	    }
    	return lengthSq;
    }

	public double distance(VPoint point) {
		return GeometryUtils.closestToSegment(this, point).distance(point);
	}

	public Vector2D asVector(){
		return new Vector2D(x2 - x1, y2 - y1);
	}

	public VPoint asVPoint(){
		return new VPoint(x2 - x1, y2 - y1);
	}


	@Override
	public String toString() {
		return p1 + " - " + p2;
	}


	// Methods used by the R-Tree
	@Override
	public double distance(Rectangle r) {
		if (r.contains(x1, y1) || r.contains(x2, y2)) {
			return 0;
		} else {
			double d1 = distance(r.x1(), r.y1(), r.x1(), r.y2());
			if (d1 == 0)
				return 0;
			double d2 = distance(r.x1(), r.y2(), r.x2(), r.y2());
			if (d2 == 0)
				return 0;
			double d3 = distance(r.x2(), r.y2(), r.x2(), r.y1());
			double d4 = distance(r.x2(), r.y1(), r.x1(), r.y1());
			return Math.min(d1, Math.min(d2, Math.min(d3, d4)));
		}
	}

	private double distance(double x1, double y1, double x2, double y2) {
		VLine line = new VLine(x1, y1, x2, y2);
		double d1 = line.ptSegDist(this.x1, this.y1);
		double d2 = line.ptSegDist(this.x2, this.y2);
		VLine line2 = new VLine(this.x1, this.y1, this.x2, this.y2);
		double d3 = line2.ptSegDist(x1, y1);
		if (d3 == 0)
			return 0;
		double d4 = line2.ptSegDist(x2, y2);
		if (d4 == 0)
			return 0;
		else
			return Math.min(d1, Math.min(d2, Math.min(d3, d4)));
	}

	@Override
	public Rectangle mbr() {
    	try {
		    return Geometries.rectangle(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2));
	    } catch(IllegalArgumentException e) {
    		System.out.println("wtf?");
		    return Geometries.rectangle(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2));
	    }

	}

	@Override
	public boolean intersects(Rectangle r) {
		return RectangleUtil.rectangleIntersectsLine(r.x1(), r.y1(), r.x2() - r.x1(), r.y2() - r.y1(), x1, y1, x2, y2);
	}

	@Override
	public boolean isDoublePrecision() {
		return true;
	}

	/*@Override
	public double x1() {
		return x1;
	}

	@Override
	public double y1() {
		return y1;
	}

	@Override
	public double x2() {
		return x2;
	}

	@Override
	public double y2() {
		return y2;
	}

	@Override
	public boolean intersects(Line line) {
		com.github.davidmoten.rtree.internal.Line2D line1 = new com.github.davidmoten.rtree.internal.Line2D(x1, y1, x2, y2);
		com.github.davidmoten.rtree.internal.Line2D line2 = new com.github.davidmoten.rtree.internal.Line2D(line.x1(), line.y1(), line.x2(), line.y2());
		return line2.intersectsLine(line1);
	}

	@Override
	public boolean intersects(Point point) {
		return intersects(point.mbr());
	}

	@Override
	public boolean intersects(Circle circle) {
		return GeometryUtil.lineIntersects(x1, y1, x2, y2, circle);
	}*/
}
