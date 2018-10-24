package org.vadere.util.geometry.shapes;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRing;

public class TestVRing {

	private static VRing validRing;
	private static double allowedError;

	@BeforeClass
	public static void setUp() {
		validRing = new VRing(2, 1);
		allowedError = 0;
	}

	@Test(expected = IllegalArgumentException.class)
	public void testVRingConstructorExceptionRadius() {
		double illegalRadius = 0;

		new VRing(illegalRadius, illegalRadius);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testVRingConstructorExceptionCirclesOfDifferentSizes() {
		double radius = 2;

		new VRing(radius, radius);
	}

	@Test
	public void testGetCenter() {
		double expectedX = 0;
		double expectedY = 0;

		assertEquals(expectedX, validRing.getCenter().x, allowedError);
		assertEquals(expectedY, validRing.getCenter().y, allowedError);
	}

	@Test
	public void testGetRadiusInnerCircle() {
		double expected = 1;

		assertEquals(expected, validRing.getRadiusInnerCircle(), allowedError);
	}

	@Test
	public void testGetRadiusOuterCircle() {
		double expected = 2;

		assertEquals(expected, validRing.getRadiusOuterCircle(), allowedError);
	}

	@Test
	public void testContainsVPoint() {
		VPoint containedPoint1 = new VPoint(1.5, 0);
		VPoint containedPoint2 = new VPoint(-1.5, 0);
		VPoint containedPoint3 = new VPoint(0, 1.5);
		VPoint containedPoint4 = new VPoint(0, -1.5);

		assertTrue(validRing.contains(containedPoint1));
		assertTrue(validRing.contains(containedPoint2));
		assertTrue(validRing.contains(containedPoint3));
		assertTrue(validRing.contains(containedPoint4));
	}

	@Test
	public void testNotContainsVPoint() {
		VPoint notContainedPoint1 = new VPoint(0, 0);
		VPoint notContainedPoint2 = new VPoint(0.5, 0.5);
		VPoint notContainedPoint3 = new VPoint(2.5, 2.5);
		VPoint notContainedPoint4 = new VPoint(4, 4);

		assertFalse(validRing.contains(notContainedPoint1));
		assertFalse(validRing.contains(notContainedPoint2));
		assertFalse(validRing.contains(notContainedPoint3));
		assertFalse(validRing.contains(notContainedPoint4));
	}

	@Test
	public void testEqualsSameRing() {
		VRing sameRing = new VRing(1, 2);

		assertTrue(validRing.equals(sameRing));
	}

	@Test
	public void testEqualsDifferentCenter() {
		VRing differentRing = new VRing(new VPoint(0, 1), 1, 2);

		assertFalse(validRing.equals(differentRing));
	}

	@Test
	public void testEqualsDifferentRadii() {
		VRing differentRing = new VRing(1, 3);

		assertFalse(validRing.equals(differentRing));
	}
}
