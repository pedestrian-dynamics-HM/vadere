package org.vadere.util.geometry;

import org.junit.Test;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

import static org.junit.Assert.assertEquals;

/**
 * @author Benedikt Zoennchen
 */
public class TestPoint {

	@Test
	public void testProjection() {
		VPoint b = new VPoint(1, 0);
		VPoint a = new VPoint(0.5, 4.0);

		double alpha = a.scalarProduct(b.norm());
		VPoint a1 = b.norm().scalarMultiply(alpha);
		assertEquals(0.0, a1.getY(), GeometryUtils.DOUBLE_EPS);

		IPoint a11 = a.projectOnto(b);
		assertEquals(0.0, a11.getY(), GeometryUtils.DOUBLE_EPS);

		b = new VPoint(123.1, 12.11);
		a = new VPoint(4.315, 4.021);
		a11 = a.projectOnto(b);

		double ccw = GeometryUtils.ccw(0,0, a11.getX(), a11.getY(), b.getX(), b.getY());
		assertEquals(0.0, ccw, GeometryUtils.DOUBLE_EPS);
	}

	@Test
	public void testLineProjection() {
		VPoint p = new VPoint(132.13, -123.113);
		VPoint q = new VPoint(-134.12313, 2.0);
		VPoint a = new VPoint(31.123, 0.13133);

		IPoint a11 = GeometryUtils.projectOntoLine(a.getX(), a.getY(), p.getX(), p.getY(), q.getX(), q.getY());
		double ccw = GeometryUtils.ccw(p.getX(), p.getY(), a11.getX(), a11.getY(), q.getX(), q.getY());
		assertEquals(0.0, ccw, GeometryUtils.DOUBLE_EPS);
	}
}
