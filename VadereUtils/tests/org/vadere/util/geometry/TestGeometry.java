package org.vadere.util.geometry;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.DataPoint;
import org.vadere.util.geometry.Geometry;
import org.vadere.util.geometry.GeometryUtils;
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
		List<DataPoint> orderedList = GeometryUtils.orderByAngle(allPoints,
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

}
