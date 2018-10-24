package org.vadere.util.geometry.shapes;

import org.junit.Test;

import java.awt.geom.Path2D;

import static org.junit.Assert.*;

public class VShapeTest {

	@Test
	public void testIntersectShapesReturnsTrueWithOverlappingShapes() {
		VShape a = new VRectangle(0, 0, 1, 1);
		VShape b = new VRectangle(0, 0, 1, 1);

		assertTrue(a.intersects(b));
	}

	@Test
	public void testIntersectShapesReturnsFalseWithNonOverlappingShapes() {
		VShape a = new VRectangle(0, 0, 1, 1);
		VShape b = new VRectangle(1.1, 0, 1, 1);

		assertFalse(a.intersects(b));
	}

	@Test
	public void testIntersectShapesReturnsTrueWithOverlappingCircles() {
		VShape a = new VRectangle(0, 0, 1, 1);
		VShape b = new VCircle(new VPoint(-1, -1), 5.0);

		assertTrue(a.intersects(b));
	}

	@Test
	public void testOverlapWithCircles() {
		VShape a = new VCircle(new VPoint(503.9265351385102, 506.9174145081969), 0.195);
		VShape b = new VCircle(new VPoint(504.19098333791044, 506.8493305279853), 0.195);

		assertTrue(a.intersects(b));
	}

	/**
	 * Centroid and CirumCircle are the same for VRectangle
	 */
	@Test
	public void testCentroidOnVRectangle() {
		VShape a = new VRectangle(500.333, 500.0, 12, 12);
		VPoint center_CirumCircle = a.getCircumCircle().getCenter();
		VPoint center_Centroid = a.getCentroid();
		VPoint diff = center_Centroid.subtract(center_CirumCircle);

		assertTrue("Centroid Center and CircumCircle Center not the same for VRectangle", Math.abs(diff.x) < 0.001);
		assertTrue("Centroid Center and CircumCircle Center not the same for VRectangle", Math.abs(diff.y) < 0.001);
	}

	@Test
	public void testCentroidOnVPolygon() {
		Path2D.Double path = new Path2D.Double();
		path.moveTo(500.0, 500.0);
		path.lineTo(510.0, 490.0);
		path.lineTo(490.0, 490.0);
		path.lineTo(500.0, 500.0);
		path.closePath();
		VPolygon polygon = new VPolygon(path); // gleichseitiges dreieck schwerpunkt 1/3 der hoehe
		VPoint target = new VPoint(500.0, 490.0 + 10.0 / 3);
		VPoint diff = target.subtract(polygon.getCentroid());
		assertTrue("Centroid Center and CircumCircle Center not the same for VRectangle", Math.abs(diff.x) < 0.001);
		assertTrue("Centroid Center and CircumCircle Center not the same for VRectangle", Math.abs(diff.y) < 0.001);

	}

}