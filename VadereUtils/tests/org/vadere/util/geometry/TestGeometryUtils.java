package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestGeometryUtils {

	@Before
	public void setUp(){}

	@Test
	public void testCCW() {
		VPoint p1 = new VPoint(0,0);
		VPoint p2 = new VPoint(0, 1);

		VPoint r = new VPoint(5, 0.5);

		VPoint q = new VPoint(5, -0.5);

		VPoint t = new VPoint(-5, -0.5);

		assertTrue(GeometryUtils.isRightOf(p1, p2, r));

		assertTrue(!GeometryUtils.isRightOf(p2, p1, r));

		assertTrue(GeometryUtils.isRightOf(p1, p2, q));

		assertTrue(!GeometryUtils.isRightOf(p2, p1, q));

		assertFalse(GeometryUtils.isRightOf(p1, p2, t));

		assertFalse(!GeometryUtils.isRightOf(p2, p1, t));
	}

	@Test
	public void testCircumcenterOrder() {
		double eps = 0.000001;
		VPoint r = new VPoint(5, 0.5);
		VPoint q = new VPoint(5, -0.5);
		VPoint t = new VPoint(-5, -0.5);

		VPoint c1 = GeometryUtils.getCircumcenter(r, q, t);
		VPoint c2 = GeometryUtils.getCircumcenter(r, t, q);
		VPoint c3 = GeometryUtils.getCircumcenter(t, r, q);

		assertTrue(c1.distance(c2) < eps);
		assertTrue(c2.distance(c3) < eps);
	}
}
