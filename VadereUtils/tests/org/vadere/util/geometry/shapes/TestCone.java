package org.vadere.util.geometry.shapes;

import org.junit.Test;
import org.vadere.util.geometry.shapes.VCone;
import org.vadere.util.geometry.shapes.VPoint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class TestCone {

	@Test
	public void testEdgeIterator() {
		// 90 degree cone
		VCone cone = new VCone(new VPoint(0,0), new VPoint(1, 1), Math.PI/2);
		assertTrue(cone.contains(new VPoint(1, 1)));
		assertTrue(cone.contains(new VPoint(5, 7)));
		assertTrue(cone.contains(new VPoint(5, 7000)));
		assertTrue(cone.contains(new VPoint(1, 0.00001)));
		assertTrue(cone.contains(new VPoint(0.00001, 1)));


		assertFalse(cone.contains(new VPoint(-1, -1)));
		assertFalse(cone.contains(new VPoint(1, -0.00001)));
		assertFalse(cone.contains(new VPoint(-0.00001, 1)));

		assertFalse(cone.contains(new VPoint(-1, 1)));
		assertFalse(cone.contains(new VPoint(1, -1)));
		assertFalse(cone.contains(new VPoint(-5, -5)));
		assertFalse(cone.contains(new VPoint(1, -1)));
	}

}
