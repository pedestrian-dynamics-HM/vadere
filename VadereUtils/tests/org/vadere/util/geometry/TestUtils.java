package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Benedikt Zoennchen
 */
public class TestUtils {

	private double eps;

	@Before
	public void setUp(){
		eps = 0.000001;
	}

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
		VPoint r = new VPoint(5, 0.5);
		VPoint q = new VPoint(5, -0.5);
		VPoint t = new VPoint(-5, -0.5);

		VPoint c1 = GeometryUtils.getCircumcenter(r, q, t);
		VPoint c2 = GeometryUtils.getCircumcenter(r, t, q);
		VPoint c3 = GeometryUtils.getCircumcenter(t, r, q);

		assertTrue(c1.distance(c2) < eps);
		assertTrue(c2.distance(c3) < eps);
	}

	@Test
	public void testAngle() {
		VPoint p = new VPoint(1,1);
		VPoint r = new VPoint(1,-1);
		VPoint zero = new VPoint(0,0);
		VPoint q = new VPoint(1, 0);

		assertEquals(1.0/4.0 * Math.PI, GeometryUtils.angle(p, zero, q), eps);
		assertEquals(1.0/4.0 * Math.PI, GeometryUtils.angle(q, zero, p), eps);

		assertEquals(1.0/4.0 * Math.PI, GeometryUtils.angle(r, zero, q), eps);
		assertEquals(1.0/4.0 * Math.PI, GeometryUtils.angle(q, zero, r), eps);
	}

	@Test
	public void testIntersectionPointComputation() {
		VPolygon polygon1 = GeometryUtils.toPolygon(new VPoint(0,0), new VPoint(1,1), new VPoint(1, 0));
		VPolygon polygon2 = GeometryUtils.toPolygon(new VPoint(0,0), new VPoint(0.5,2), new VPoint(1, 0));

		List<VPolygon> polygons = Arrays.asList(polygon1, polygon2);

		Set<VPoint> intersectionPoints = GeometryUtils.getIntersectionPoints(polygons);
		Set<VPoint> expextedIntersectionPoints = new HashSet<>();
		expextedIntersectionPoints.add(new VPoint(0,0));
		expextedIntersectionPoints.add(new VPoint(1, 1));

		assertEquals(1, intersectionPoints.size());
	}
}
