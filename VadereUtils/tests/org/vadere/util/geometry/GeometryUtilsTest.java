package org.vadere.util.geometry;

import org.junit.Test;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.geom.Path2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class GeometryUtilsTest {


	/**
	 * Test sign of area based on point order
	 */
	@Test
	public void signedArea(){
		Path2D.Double path = new Path2D.Double();
		path.moveTo(500.0, 500.0);
		path.lineTo(510.0, 490.0);
		path.lineTo(490.0, 490.0);
		path.lineTo(500.0, 500.0);path.closePath();
		VPolygon polygon = new VPolygon(path);

		assertTrue( GeometryUtils.signedAreaOfPolygon(polygon.getPoints()) < 0) ;
		assertTrue(GeometryUtils.signedAreaOfPolygon(polygon.revertOrder().getPoints()) > 0);
	}

	@Test
	public void testSignedAreaQuadrilateral() {
		double[] x = new double[]{0, 10, 10, 0};
		double[] y = new double[]{0,0, 3, 3};
		double[] z = new double[]{0, 0, 4, 4};

		assertEquals(50, GeometryUtils.areaOfPolygon(x, y, z), GeometryUtils.DOUBLE_EPS);

		x = new double[]{0+5, 10+5, 10+5, 0+5};
		y = new double[]{0+5, 0+5, 3+5, 3+5};
		z = new double[]{0+5, 0+5, 4+5, 4+5};

		assertEquals(50, GeometryUtils.areaOfPolygon(x, y, z), GeometryUtils.DOUBLE_EPS);
	}

	@Test
	public void testDistanceToLineSegment() {
		VPoint p = new VPoint(0,0);
		VPoint q = new VPoint(2, 0);

		VPoint p1 = new VPoint(0,0);
		VPoint p2 = new VPoint(1, 1);

		VLine line1 = new VLine(p, q);
		VLine line2 = new VLine(p1, p2);

		assertEquals(0, GeometryUtils.distanceToLineSegment(p, q, 1, 0), GeometryUtils.DOUBLE_EPS);
		assertEquals(1, GeometryUtils.distanceToLineSegment(p, q, 2, 1), GeometryUtils.DOUBLE_EPS);
		assertEquals(Math.sqrt(1*1 + 4*4), GeometryUtils.distanceToLineSegment(p, q, 6, -1), GeometryUtils.DOUBLE_EPS);

		assertEquals(Math.sqrt(3*3 + 3*3), GeometryUtils.distanceToLineSegment(p1, p2, 4,4), GeometryUtils.DOUBLE_EPS);
		assertEquals(Math.sqrt(4*4 + 4*4), GeometryUtils.distanceToLineSegment(p1, p2, -4,-4), GeometryUtils.DOUBLE_EPS);
		assertEquals(Math.sqrt(0.5 * 0.5 + 0.5 * 0.5), GeometryUtils.distanceToLineSegment(p1, p2, 1,0), GeometryUtils.DOUBLE_EPS);

		assertEquals(line1.distance(new VPoint(3.21, -123.123)), GeometryUtils.distanceToLineSegment(p, q, 3.21, -123.123), GeometryUtils.DOUBLE_EPS);
		assertEquals(line2.distance(new VPoint(313.21, 3.123)), GeometryUtils.distanceToLineSegment(p1, p2, 313.21, 3.123), GeometryUtils.DOUBLE_EPS);
	}

	@Test
	public void testDistanceToLine() {
		VPoint p = new VPoint(0,0);
		VPoint q = new VPoint(2, 0);

		VPoint p1 = new VPoint(0,0);
		VPoint p2 = new VPoint(1, 1);

		assertEquals(0, GeometryUtils.distanceToLine(p, q, 1, 0), GeometryUtils.DOUBLE_EPS);
		assertEquals(1, GeometryUtils.distanceToLine(p, q, 2, 1), GeometryUtils.DOUBLE_EPS);
		assertEquals(1, GeometryUtils.distanceToLine(p, q, 6, -1), GeometryUtils.DOUBLE_EPS);
		assertEquals(4, GeometryUtils.distanceToLine(p, q, 4,4), GeometryUtils.DOUBLE_EPS);

		assertEquals(0, GeometryUtils.distanceToLine(p1, p2, 4,4), GeometryUtils.DOUBLE_EPS);
		assertEquals(0, GeometryUtils.distanceToLine(p1, p2, -4,-4), GeometryUtils.DOUBLE_EPS);
		assertEquals(Math.sqrt(0.5 * 0.5 + 0.5 * 0.5), GeometryUtils.distanceToLine(p1, p2, 1,0), GeometryUtils.DOUBLE_EPS);
	}

	@Test
	public void testHalfLineIntersect() {

		VPoint p1 = new VPoint(0, 0);
		VPoint p2 = new VPoint(3, 3);

		assertTrue(GeometryUtils.intersectHalfLineSegment(new VPoint(1, 0), new VPoint(0, 1), p1, p2));
		assertTrue(GeometryUtils.intersectHalfLineSegment(new VPoint(1, 0), new VPoint(0.5, 0.5), p1, p2));
		assertTrue(GeometryUtils.intersectHalfLineSegment(new VPoint(1, 0), new VPoint(1, 1), p1, p2));
		assertTrue(GeometryUtils.intersectHalfLineSegment(new VPoint(2, 1), new VPoint(-1, 31), p1, p2));
	}

	@Test
	public void testCentroidOnTriangle() {
		Path2D.Double path = new Path2D.Double();
		path.moveTo(500.0, 500.0);
		path.lineTo(510.0, 490.0);
		path.lineTo(490.0, 490.0);
		path.lineTo(500.0, 500.0);
		path.closePath();
		VPolygon polygon = new VPolygon(path); // gleichseitiges dreieck schwerpunkt 1/3 der hoehe
		VPoint target = new VPoint(500.0, 490.0 + 10.0 / 3);
		VPoint diff = target.subtract(polygon.getCentroid());
		assertTrue("Centroid Center and CircumCircle Center not the same for Triangle", Math.abs(diff.x) < 0.001);
		assertTrue("Centroid Center and CircumCircle Center not the same for Triangle", Math.abs(diff.y) < 0.001);
	}

	/**
	 * Centroid and CirumCircle are the same for VRectangle
	 */
	@Test
	public void testCentroidOnRectangle() {
		VShape a = new VRectangle(500.333, 500.0, 12, 12);
		VPoint center_CirumCircle = a.getCircumCircle().getCenter();
		VPoint center_Centroid = a.getCentroid();
		VPoint diff = center_Centroid.subtract(center_CirumCircle);
		System.out.print(center_Centroid);
		System.out.print(center_CirumCircle);
		assertTrue("Centroid Center and CircumCircle Center not the same for VRectangle", Math.abs(diff.x) < 0.001);
		assertTrue("Centroid Center and CircumCircle Center not the same for VRectangle", Math.abs(diff.y) < 0.001);
	}

}