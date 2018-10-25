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


}