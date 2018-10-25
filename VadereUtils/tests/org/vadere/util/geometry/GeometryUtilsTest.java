package org.vadere.util.geometry;

import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.geom.Path2D;

import static org.junit.Assert.*;

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