package org.vadere.util.geometry.shapes;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * A triangle. Points must be given in counter clockwise manner to get correct
 * inward facing normals.
 *
 */
public class VTriangle extends VPolygon {

    /**
     * generated serial version uid
     */
    private static final long serialVersionUID = 5864412321949258915L;

    public final VPoint p1;
    public final VPoint p2;
    public final VPoint p3;

    public final VLine[] lines;

    private double area;

    /**
     * The centroid will be saved for performance boost, since this object is immutable.
     */
    private VPoint centroid;

    private VPoint center;

    private VPoint incenter;

    private VPoint orthocenter;

    private double radius = -1;

    /**
     * Creates a triangle. Points must be given in ccwRobust order.
     *
     * @param p1 first point of the triangle
     * @param p2 second point of the triangle
     * @param p3 third point of the triangle
     */
    public VTriangle(@NotNull final VPoint p1, @NotNull final VPoint p2, @NotNull final VPoint p3) {
        super(GeometryUtils.polygonFromPoints2D(p1, p2, p3));

        if(p1.equals(p2) || p1.equals(p3) || p2.equals(p3)) {
            throw new IllegalArgumentException("" + p1 + p2 + p3 + " is not a feasible set of points.");
        }
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
		this.area = 0;
        lines = new VLine[]{ new VLine(p1, p2), new VLine(p2, p3), new VLine(p3,p1) };
    }

    @Override
    public boolean contains(final IPoint point) {
        return GeometryUtils.triangleContains(p1, p2, p3, point);
    }

	@Override
	public double getArea() {
    	return Math.abs(getSignedArea());
	}

	public double getSignedArea() {
    	if(area == 0) {
    		area = GeometryUtils.signedAreaOfPolygon(p1, p2, p3);
	    }
    	return area;
	}

	// TODO: find better name
    public boolean isPartOf(final IPoint point, final double eps) {
        double d1 = GeometryUtils.ccw(point, p1, p2);
        double d2 = GeometryUtils.ccw(point, p2, p3);
        double d3 = GeometryUtils.ccw(point, p3, p1);
        return (d1 <= eps && d2 <= eps && d3 <= eps) || (d1 >= -eps && d2 >= -eps && d3 >= -eps);
    }

    public VPoint midPoint() {
    	return GeometryUtils.getTriangleMidpoint(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
    }

    public boolean isLine() {
        VLine l1 = new VLine(p1, p2);
        VLine l2 = new VLine(p1, p3);
        VLine l3 = new VLine(p2, p3);

        return l1.ptSegDist(p3) < GeometryUtils.DOUBLE_EPS
                || l2.ptSegDist(p2) < GeometryUtils.DOUBLE_EPS
                || l3.ptSegDist(p1) < GeometryUtils.DOUBLE_EPS;
    }

    public boolean isNonAcute() {
        double angle1 = GeometryUtils.angle(p1, p2, p3);
        double angle2 = GeometryUtils.angle(p2, p3, p1);
        double angle3 = GeometryUtils.angle(p3, p1, p2);

        // non-acute triangle
        double maxAngle = Math.max(Math.max(angle1, angle2), angle3);
        double rightAngle = Math.PI/2;
        return maxAngle > rightAngle;
    }

    @Override
    public VPoint getCentroid() {
        if(centroid == null) {
            centroid = super.getCentroid();
        }
        return centroid;
    }

    public VPoint getIncenter(){
        if(incenter == null) {
            incenter = GeometryUtils.getIncenter(p1, p2, p3);
        }

        return incenter;
    }

	public double getIncircleRadius(){
		return GeometryUtils.getIncircleRaduis(p1, p2, p3);
	}

    public VPoint getOrthocenter() {

        if(orthocenter == null) {

            VPoint p12, p13, p23, L1, L2;

            // create edge vectors
            p12 = p2.subtract(p1); // Vector2D better??
            p13 = p3.subtract(p1);
            p23 = p3.subtract(p2);

            // create system of equations
            double cross = p13.crossProduct(p12);

            L1 = new VPoint(-cross* p23.getY() , cross * p23.getX() );
            L2 = new VPoint(-cross* p13.getY() , cross * p13.getX() );

            // solve system of equation (determine first element of resulting vector lamda with cramers rule; second element not necessary)
            double lamda1 = ( p12.getX() * L2.getY() - p12.getY() * L2.getX() ) / ( L1.getX()*L2.getY() - L2.getX()*L1.getY() ) ;

            orthocenter = new VPoint ( p1.add( L1.scalarMultiply(lamda1) ) );
        }

        //VPoint orthocenter2 = getOrthocenterSlowImplementation() ;
        //assert Math.abs( orthocenter.distance(orthocenter2)) < GeometryUtils.DOUBLE_EPS ;
        return orthocenter;
    }

    public VPoint getOrthocenterSlowImplementation() {

        // create edge vectors
        VPoint p12 = p2.subtract(p1); // Vector2D better??
        VPoint p13 = p3.subtract(p1);
        VPoint p23 = p3.subtract(p2);

        // edge vectors reverse
        VPoint p21 = new VPoint(p12.scalarMultiply(-1.0));
        VPoint p31 = new VPoint(p13.scalarMultiply(-1.0));
        VPoint p32 = new VPoint(p23.scalarMultiply(-1.0));

        // calculate angles a1, a2, a3
        double a1 = Math.atan2(Math.abs( p13.crossProduct(p12)), p13.scalarProduct(p12)) ;
        double a2 = Math.atan2(Math.abs( p23.crossProduct(p21)), p23.scalarProduct(p21)) ;
        double a3 = Math.atan2(Math.abs( p31.crossProduct(p32)), p31.scalarProduct(p32)) ;

        double div = Math.tan(a1)+ Math.tan(a2)+ Math.tan(a3) ;
        double  x = ( Math.tan(a1)*p1.getX() + Math.tan(a2)*p2.getX() + Math.tan(a3)*p3.getX() ) / div;
        double  y = ( Math.tan(a1)*p1.getY() + Math.tan(a2)*p2.getY() + Math.tan(a3)*p3.getY() ) / div;

        return new VPoint(x,y);

    }

    public VPoint closestPoint(final IPoint point) {

        VPoint currentClosest = null;
        double currentMinDistance = java.lang.Double.MAX_VALUE;

        for(VLine line : lines) {
            VPoint p = GeometryUtils.closestToSegment(line, point);
            if(p.distance(point) < currentMinDistance) {
                currentMinDistance = p.distance(point);
                currentClosest = p;
            }
        }

        return currentClosest;
    }

    public VPoint getCircumcenter(){
        if(center == null) {
            center = GeometryUtils.getCircumcenter(p1, p2, p3);
        }
        return center;
    }

    public double getCircumscribedRadius() {
    	if(radius == -1) {
    		radius = getCircumcenter().distance(p1);
	    }
        return radius;
    }

    public boolean isInCircumscribedCycle(final IPoint point) {
        return getCircumcenter().distance(point) < getCircumscribedRadius();
    }

    public Stream<VLine> streamLines() {
        return Arrays.stream(getLines());
    }

    public Stream<VPoint> streamPoints() {
	    return Arrays.stream(new VPoint[]{p1, p2, p3});
    }

    public double getRadiusEdgeRatio() {
		// (1) find shortest line
    	VLine shortestLine;
    	if(lines[0].length() <= lines[1].length()) {
			if(lines[0].length() <= lines[2].length()) {
				shortestLine = lines[0];
			} else {
				shortestLine = lines[2];
			}
		} else {
		    if(lines[1].length() <= lines[2].length()) {
			    shortestLine = lines[1];
		    } else {
			    shortestLine = lines[2];
		    }
	    }

    	return getCircumscribedRadius() / shortestLine.length();
    }

    public VLine[] getLines() {
        return lines;
    }

    @Override
    public String toString() {
        return "["+p1 + "," + p2 + "," + p3 + "]";
    }

}
