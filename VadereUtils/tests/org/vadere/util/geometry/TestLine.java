package org.vadere.util.geometry;

import static org.junit.Assert.assertEquals;

import javax.sound.sampled.Line;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

/**
 * Basic tests of the {@link Line} class.
 * 
 * 
 */
public class TestLine {

	public static VLine lineToTest1;
	public static VLine lineToTest2;
	public static VLine lineToTest3;
	public static VLine lineToTest4;
	public static VLine lineToTest5;
	public static VLine lineToTest6;

	public static Geometry geometry1;
	public static VPolygon obstacle1;
	public static VPolygon obstacle2;

	@Before
	public void setUp() throws Exception {

		lineToTest1 = new VLine(new VPoint(0, 0), new VPoint(100, 100));
		lineToTest2 = new VLine(new VPoint(50, 50), new VPoint(0, 100));
		lineToTest3 = new VLine(new VPoint(0, 100), new VPoint(0, 0));

		geometry1 = new Geometry();
		obstacle1 = GeometryUtils.polygonFromPoints2D(
				// create a small rectangle that line1 crosses through its edges
				new VPoint(50, 50), new VPoint(60, 50), new VPoint(60, 60),
				new VPoint(50, 60));
		// create another obstacle that touches the first to check touching
		// behaviour
		obstacle2 = GeometryUtils.polygonFromPoints2D(new VPoint(55, 40),
				new VPoint(65, 40), new VPoint(65, 50), new VPoint(55, 50));

		geometry1.addPolygon(obstacle1);
		geometry1.addPolygon(obstacle2);

		lineToTest4 = new VLine(new VPoint(50, 0), new VPoint(50, 100));
		lineToTest5 = new VLine(new VPoint(60, 70), new VPoint(40, 50));
		lineToTest6 = new VLine(new VPoint(40, 50), new VPoint(70, 50));
	}

	@Test
	public void testLineIntersects() {
		assertEquals("Line 1 should not be said to intersect line 2.", true,
				lineToTest1.intersectsLine(lineToTest2));
		assertEquals("Line 1 should not be said to intersect line 3.", true,
				lineToTest1.intersectsLine(lineToTest3));
		assertEquals("Line 1 should not be said to intersect itself.", true,
				lineToTest1.intersectsLine(lineToTest1));
	}

	@Test
	public void testLineIntersectsGeometryThroughEdges() {
		// the line should intersect the square
		assertEquals(true, geometry1.intersect(lineToTest1));
	}

}
