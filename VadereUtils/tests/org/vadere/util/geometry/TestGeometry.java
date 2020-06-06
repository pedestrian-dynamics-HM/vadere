package org.vadere.util.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * Basic unit test for the {@link Geometry} class. NOT complete.
 * 
 * 
 */
public class TestGeometry {

	public static Geometry toTest;

	public static final double roomSideLen = 100;

	@Before
	public void setUp() throws Exception {
		toTest = CreateGeometry.createRoomWithoutObstacles(roomSideLen);
	}

	@Test
	public void testGetPoints() {
		assertEquals("Number of points does not match.", 4, toTest.getPoints()
				.size());
	}

	@Test
	public void testOrderByAngle() {
		List<VPoint> allPoints = toTest.getPoints();
		List<DataPoint> orderedList = orderByAngle(allPoints,
				new VPoint(roomSideLen / 2, roomSideLen / 2));

		List<VPoint> testList = new LinkedList<VPoint>();
		for (DataPoint d : orderedList) {
			testList.add(new VPoint(d.getX(), d.getY()));
		}

		assertEquals(new VPoint(roomSideLen, roomSideLen), testList.get(0));
		assertEquals(new VPoint(0, roomSideLen), testList.get(1));
		assertEquals(new VPoint(0, 0), testList.get(2));
		assertEquals(new VPoint(roomSideLen, 0), testList.get(3));
	}

	@Test
	public void testLineCircleIntersectionZeroResults() {
		VCircle circle = new VCircle(1, 1, 1);
		VLine line = new VLine(3,0, 4, 1);
		VPoint[] intersectionPoints = GeometryUtils.intersection(line, circle);
		assertTrue(intersectionPoints.length == 0);
	}

	@Test
	public void testLineCircleIntersectionTwoResults() {
		VCircle circle = new VCircle(3, 4.1, 3);
		VLine line = new VLine(3,4.1, 4, 5.1);
		VPoint[] intersectionPoints = GeometryUtils.intersection(line, circle);
		// computed with http://www.ambrsoft.com/TrigoCalc/Circles2/circlrLine_.htm
		VPoint[] expectedIntersectionPoints = new VPoint[]{
				new VPoint(5.121, 6.221),
				new VPoint(0.879, 1.979)};

		assertTrue(intersectionPoints.length == 2);
		assertTrue(testPointListEquality(expectedIntersectionPoints, intersectionPoints, 0.001));
	}

	@Test
	public void testLineCircleIntersectionOneResults() {
		VCircle circle = new VCircle(1, 1, 1);
		VLine line = new VLine(3,0, 4, 0);
		VPoint[] intersectionPoints = GeometryUtils.intersection(line, circle);

		assertTrue(intersectionPoints.length == 1);

		VPoint[] expectedIntersectionPoints = new VPoint[]{new VPoint(1, 0)};
		assertTrue(testPointListEquality(expectedIntersectionPoints, intersectionPoints, 0.001));
	}

	@Test
	public void testGenerateAccuteTriangles() {

	}

	private static boolean testPointListEquality(final VPoint[] expectedPoints, final VPoint[] points, final double tolerance) {

		if(expectedPoints.length != points.length) {
			return false;
		}

		for(VPoint p1 : expectedPoints) {
			boolean found = false;
			for(VPoint p2 : points) {
				if(p1.equals(p2, tolerance)) {
					found = true;
					break;
				}
			}
			if(!found) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Orders a given list angular relative to a given point, starting with
	 * angle3D 0.
	 *
	 * @param allPoints
	 * @param center
	 * @return an ordered DataPoint list with the angle3D of the point as data and
	 *         the original index set.
	 */
	private static List<DataPoint> orderByAngle(List<VPoint> allPoints,
	                                           VPoint center) {
		List<DataPoint> orderedList = new ArrayList<DataPoint>();

		for (int i = 0; i < allPoints.size(); i++) {
			Vector2D p = new Vector2D(allPoints.get(i));
			orderedList.add(new DataPoint(p.x, p.y, GeometryUtils.angleTo(p, center)));
		}
		// sort by angle3D
		Collections.sort(orderedList, DataPoint.getComparator());

		return orderedList;
	}

}
