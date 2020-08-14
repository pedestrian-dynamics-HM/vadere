package org.vadere.util.geometry.shapes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.logging.Logger;
import static org.junit.Assert.*;

public class TestVTriangle {

    // following logger writes to ASCI output file VadereUtils/log.out
    private static Logger logger = Logger.getLogger(TestVTriangle.class);

    // create y-symmetric 60°-triangle
    private double radiusShould = 1.0;
    private double coor = radiusShould/Math.tan(30./180. * Math.PI);
    private double coorTop = Math.sqrt( Math.pow(radiusShould,2) + Math.pow(coor,2)  );
    private VTriangle vTriangle_symY_60degree = new VTriangle(new VPoint(-coor,-radiusShould), new VPoint(coor,-radiusShould), new VPoint(0.,coorTop) );

    // create y-symmetric 45°-45°-90°-triangle (90°-angle3D is split by y-axis)
    private VPoint vPoint1 = new VPoint(-1.,0.);
    private VPoint vPoint2 = new VPoint(1.,0.);
    private VPoint vPoint3 = new VPoint(0.,1);
    private VTriangle vTriangle_sym = new VTriangle(vPoint1, vPoint2, vPoint3);

    private VPoint vPoint4 = new VPoint(vPoint2.getX() + 0.1,vPoint2.getY()); // getter not necessary because attributes are public
    private VPoint vPoint5 = new VPoint(0.1 , 0.1 );

    // create 60°-triangle (2 vertices on y-axis)
    private VPoint vPoint7 = new VPoint(0., -0.5 * Math.sqrt(2.) );
    private VPoint vPoint8 = new VPoint(0., 0.5 * Math.sqrt(2.) );
    private VTriangle vTriangle_60degree = new VTriangle(vPoint7, vPoint2, vPoint8);

    @Before
    public void setUp() throws Exception {
    }
    @Test
    public void testClosestPoint0(){
        assertFalse( vTriangle_sym.closestPoint(vPoint1).equals(vPoint2,0.1) );
        assertTrue( vTriangle_sym.closestPoint(vPoint2).equals(vPoint2,0.1) );
        assertFalse( vTriangle_sym.closestPoint(vPoint2).equals(vPoint4,0.1) );
        assertTrue( vTriangle_sym.closestPoint(vPoint2).equals(vPoint4,0.2) );
    }

    /**
     * documentation, see: GeometryUtils.java
     * vTriangle_sym.contains is true if the triangle contains the point, otherwise false.
     */
    @Test
    public void testContains() {
        // vTriangle_sym.contains returns true if point is a vertex or point is inside triangle
        assertTrue("Point 1 is vertex of Triangle 1.",  vTriangle_sym.contains(vPoint1));
        assertTrue("Point 2 is vertex of Triangle 1.",  vTriangle_sym.contains(vPoint2));
        assertTrue("Point 3 is vertex of Triangle 1.",  vTriangle_sym.contains(vPoint3));
        assertTrue("Point 5 is inside Triangle 1.",     vTriangle_sym.contains(vPoint5));

        // vTriangle_sym.contains returns false if point is outside triangle
        assertFalse("Point 4 lays outside Triangle 1.", vTriangle_sym.contains(vPoint4));
    }

    /**
     * isPartOf( point , 0.0) == vTriangle_sym.contains(point)
     * isPartOf( point , !=0.0) -> vTriangle_sym.contains(point) + tolerance
     */
    @Test
    public void testIsPartOf() {
        // vTriangle_sym.isPartOf returns true if point is a vertex or point is inside triangle + tolerance
        double tolerance = 0.0;
        assertTrue("Point 1 is vertex of Triangle 1.", vTriangle_sym.isPartOf(vPoint1,tolerance));
        assertTrue("Point 2 is vertex of Triangle 1.", vTriangle_sym.isPartOf(vPoint2,tolerance));
        assertTrue("Point 3 is vertex of Triangle 1.", vTriangle_sym.isPartOf(vPoint3,tolerance));
        assertTrue("Point 5 is inside Triangle 1.", vTriangle_sym.isPartOf(vPoint5,tolerance));

        // vTriangle_sym.isPartOf returns false if point is outside triangle + tolerance
        assertFalse("Point 4 lays outside Triangle 1.", vTriangle_sym.isPartOf(vPoint4,tolerance));
    }

    @Test
    public void testMidPoint() {
        VPoint vPoint = vTriangle_sym.midPoint();
        double x = vPoint.getX();
        double y = vPoint.getY();
        double eps = Math.max (Math.abs(x - 0.0), Math.abs(y - 1./3));
        assertTrue(eps < GeometryUtils.DOUBLE_EPS);
    }


    @Test
    public void testIsLine() {
    }

    @Test
    public void testIsNonAcute() {
        VTriangle vTriangle = new VTriangle(vPoint2, vPoint3, vPoint5);
        assertTrue(vTriangle.isNonAcute()); // is non acute
        assertFalse(this.vTriangle_sym.isNonAcute());
    }

    @Test
    public void testGetCentroid() {
        VPoint vPoint = vTriangle_sym.getCentroid();
        VPoint checkPoint = new VPoint(0.0, 1./3); // special structure of triangle: midpoint = centroid
        assertEquals(checkPoint,vPoint);
    }

    @Test
    public void testGetIncenter(){

        // use y-symmetric 60°-triangle for test
        VPoint vPoint = vTriangle_symY_60degree.getIncenter();
        double x = vPoint.getX();
        double y = vPoint.getY();
        //assertEquals(vPoint, new VPoint(0.0,0.0), GeometryUtils.DOUBLE_EPS);
        double eps = Math.max (Math.abs(x - 0.0), Math.abs(y - 0.0)); // Point (0,0) is incenter
        assertTrue(eps < GeometryUtils.DOUBLE_EPS);

    }

    @Test
    public void testGetOrthocenter() {
        // time measure
        long startTime, runTimeInNs, runTimeInNsSlow;
        VPoint vPointY, vPoint;

        /** Triangle 1: use 60°-triangle (2 vertices on y-axis) for test */
        // fast implementation
        startTime = System.nanoTime();
        vPoint = vTriangle_60degree.getOrthocenter();
        runTimeInNs = System.nanoTime() - startTime ;
        double x = vPoint.getX();
        double y = vPoint.getY();
        double eps = Math.max (Math.abs(x - 0.5), Math.abs(y - 0.0)); // Point (0.5,0) is orthocenter
        assertTrue(eps < GeometryUtils.DOUBLE_EPS);

        // slow implementation
        startTime = System.nanoTime();
        vPoint = vTriangle_60degree.getOrthocenterSlowImplementation();
        runTimeInNsSlow = System.nanoTime() - startTime ;
        x = vPoint.getX();
        y = vPoint.getY();
        eps = Math.max (Math.abs(x - 0.5), Math.abs(y - 0.0)); // Point (0.5,0) is orthocenter
        assertTrue(eps < GeometryUtils.DOUBLE_EPS);

        // compare fast and slow implementation
        logger.info("Triangle 1: Fast implementation required " + runTimeInNs + "[nano sec]");
        logger.info("Triangle 1: Slow implementation required " + runTimeInNsSlow + "[nano sec]");
        logger.info("Triangle 1: time factor -> slow to fast: " + 1.0*runTimeInNsSlow/runTimeInNs );

        /** Triangle 2:  triangle with y-symmetry (45°-90°-45°); does not work because of infinite slope */
        // fast implementation
        startTime = System.nanoTime();
        vPointY= this.vTriangle_sym.getOrthocenter();
        runTimeInNs = System.nanoTime() - startTime ;
        x = vPointY.getX();
        y = vPointY.getY();
        eps = Math.max (Math.abs(x - 0.0), Math.abs(y - 1.0)); // Point (0.5,0) is orthocenter
        assertTrue(eps < GeometryUtils.DOUBLE_EPS);

        // slow implementation
        startTime = System.nanoTime();
        vPointY = this.vTriangle_sym.getOrthocenterSlowImplementation();
        runTimeInNsSlow = System.nanoTime() - startTime ;
        x = vPointY.getX();
        y = vPointY.getY();
        eps = Math.max (Math.abs(x - 0.0), Math.abs(y - 1.0)); // Point (0.5,0) is orthocenter
        assertTrue(eps < GeometryUtils.DOUBLE_EPS);

        // compare fast and slow implementation
        logger.info("Triangle 2: Fast implementation required " + runTimeInNs + "[nano sec]");
        logger.info("Triangle 2: Slow implementation required " + runTimeInNsSlow + "[nano sec]");
        logger.info("Triangle 2: time factor -> slow to fast: " + 1.0*runTimeInNsSlow/runTimeInNs );
    }

    @Test
    public void testClosestPoint() {
        // y-symmetric triangle contains vPoint1
        assertEquals(vTriangle_sym.closestPoint(vPoint1), vPoint1 );

        // 60°-triangle (2 vertices on y-axis), vPoint1 is outside
        VPoint newPoint = vTriangle_60degree.closestPoint(vPoint1);
        assertEquals(new VPoint(0.0 , 0.0 ), newPoint ); // newPoint is projection of vPoint1 on y-Axis

        // 60°-triangle (2 vertices on y-axis), vPoint5 is inside
        VPoint newPoint2 = vTriangle_60degree.closestPoint(vPoint5); // newPoint2 is projection of vPoint1 on y-Axis
        double x = newPoint2.getX();
        double y = newPoint2.getY();
        double eps = Math.max (Math.abs(x - 0.0), Math.abs(y - 0.1));
        assertTrue(eps < GeometryUtils.DOUBLE_EPS);

        // 60°-triangle (2 vertices on y-axis), vPoint5 is inside
        VPoint newPoint3 = vTriangle_60degree.closestPoint(new VPoint(0.0,0.3)); // newPoint2 is projection of vPoint1 on y-Axis
        x = newPoint3.getX();
        y = newPoint3.getY();
        eps = Math.max (Math.abs(x - 0.0), Math.abs(y - 0.3));
        assertTrue(eps < GeometryUtils.DOUBLE_EPS);
    }


    @Test
    public void testGetCircumcenter(){
        // y-symmetric 60°-triangle: Circumcenter = (0.0,0.0)
        VPoint vPoint = vTriangle_symY_60degree.getIncenter();
        double x = vPoint.getX();
        double y = vPoint.getY();
        double eps = Math.max (Math.abs(x - 0.0), Math.abs(y - 0.0));
        assertTrue(eps < GeometryUtils.DOUBLE_EPS);
    }

    @Test
    public void testGetCircumscribedRadius() {
        // symmetric triangle
        double radius = vTriangle_sym.getCircumscribedRadius();
        double eps = Math.abs(radius - 1.0) ;
        assertTrue(eps < GeometryUtils.DOUBLE_EPS);
    }

    @After
    public void tearDown() throws Exception {
    }
}