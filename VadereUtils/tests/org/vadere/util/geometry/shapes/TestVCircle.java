package org.vadere.util.geometry.shapes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Rectangle2D;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

public class TestVCircle {

	private VCircle testCircleOrigin;
	private VCircle testCircle1;

	@Before
	public void setUp() {
		this.testCircleOrigin = new VCircle(0.5);
		testCircle1 = new VCircle(1.2, -2.4, 0.9);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativeRadius() {
		new VCircle(-0.1);
	}

	@Test
	public void testContainsPoint() {
		assertTrue(testCircleOrigin.contains(new VPoint(0.4999999, 0)));
		assertTrue(testCircleOrigin.contains(new VPoint(0, 0.4999999)));
		assertTrue(testCircleOrigin.contains(new VPoint(0.2, 0.2)));
		assertTrue(testCircle1.contains(new VPoint(0.3, -2.4)));
	}

	@Test
	public void testDistanceToPoint() {
		assertEquals(testCircleOrigin.distance(new VPoint(0.5, 0)), 0,
				GeometryUtils.DOUBLE_EPS);
		assertEquals(testCircleOrigin.distance(new VPoint(0, 0.5)), 0,
				GeometryUtils.DOUBLE_EPS);
	}

	@Test
	public void testEquals() {
		VCircle otherEqual1 = new VCircle(1.2, -2.4, 0.9);
		assertTrue(testCircle1.equals(otherEqual1));
	}

	@Test
	public void testIntersects() {
		assertTrue(testCircleOrigin.intersects(new Rectangle2D.Double(0, 0, 1,
				1)));
		assertTrue(testCircleOrigin.intersects(new Rectangle2D.Double(-0.5,
				-0.5, 1, 1)));
		assertTrue(testCircleOrigin.intersects(new Rectangle2D.Double(0, 0,
				0.1, 0.1)));
		assertTrue(testCircleOrigin.intersects(new Rectangle2D.Double(0.5, 0,
				1, 1)));
	}

	@Test
	public void testClosestPoint() {
		assertEquals(testCircleOrigin.closestPoint(new VPoint(1, 0)),
				new VPoint(0.5, 0));
		assertEquals(testCircleOrigin.closestPoint(new VPoint(0, 1)),
				new VPoint(0, 0.5));
	}

}
