package org.vadere.state.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SpawnArrayTest {

	private VRectangle source;
	private VRectangle elementBound;
	private SpawnArray spawnArray;

	@Test
	public void NumberOfElements() {
		// Number of Elements
		source = new VRectangle(1.0, 1.0, 9.9, 9.9);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);
		assertEquals("expected spawn points don't match", 81, spawnArray.getSpawnPoints().length);
	}

	@Test
	public void toSmallSource1() {
		// Number of Elements
		TestAppender testAppender = new TestAppender();
		Logger log = LogManager.getLogger(SpawnArray.class);
		log.addAppender(testAppender);

		source = new VRectangle(1.0, 1.0, 0.1, 0.1);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);
		assertEquals("expected spawn points don't match", 1, spawnArray.getSpawnPoints().length);

		String msg = String.format("Dimension of Source is to small for at least one" +
						" dimension to contain designated spawnElement with" +
						" Bound (%.2f x %.2f) Set to (%d x %d)",
				1.0, 1.0, 1, 1);
		assertEquals("", testAppender.getLog().get(0).getMessage(), msg);
		log.removeAppender(testAppender);
	}

	@Test
	public void toSmallSource2() {
		// Number of Elements
		TestAppender testAppender = new TestAppender();
		Logger log = LogManager.getLogger(SpawnArray.class);
		log.addAppender(testAppender);

		source = new VRectangle(1.0, 1.0, 3.0, 0.1);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);
		assertEquals("expected spawn points don't match", 3, spawnArray.getSpawnPoints().length);
		String msg = String.format("Dimension of Source is to small for at least one" +
						" dimension to contain designated spawnElement with" +
						" Bound (%.2f x %.2f) Set to (%d x %d)",
				1.0, 1.0, 3, 1);
		assertEquals("", testAppender.getLog().get(0).getMessage(), msg);
		log.removeAppender(testAppender);
		log.removeAppender(testAppender);
	}

	/**
	 * Group dimension is set as W x H for a Group of 6 this would be 3x2 Test if Source Width is
	 * smaller than 3! Here the Group must be 2x3
	 */
	@Test
	public void groupXDimBiggerThanSource() {
		source = new VRectangle(1.0, 1.0, 2.0, 7);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);

		VPoint[] spawnPoints = spawnArray.getSpawnPoints();
		LinkedList<VPoint> points = spawnArray.getNextGroup(6, createMock(0.5));
		assertEquals(spawnPoints[0], points.pollFirst());
		assertEquals(spawnPoints[1], points.pollFirst());
		assertEquals(spawnPoints[2], points.pollFirst());
		assertEquals(spawnPoints[3], points.pollFirst());
		assertEquals(spawnPoints[4], points.pollFirst());
		assertEquals(spawnPoints[5], points.pollFirst());

	}

	@Test
	public void Points() {
		source = new VRectangle(1.0, 1.0, 9.9, 9.9);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);

		// first Point
		List<DynamicElement> dynamicElements = createMock(0.5);
		VPoint p = spawnArray.getNextSpawnPoints(1, dynamicElements).getFirst();
		assertEquals("Point does not match", p, new VPoint(1.5, 1.5));
		assertEquals("Next Element does not match", 1, spawnArray.getNextSpawnPointIndex());

		// 10 more points
		IntStream.range(0, 10).forEach(i -> spawnArray.getNextSpawnPoints(1, dynamicElements));
		assertEquals("Next Element does not match", 11, spawnArray.getNextSpawnPointIndex());
		VPoint first = new VPoint(source.x + elementBound.width / 2, source.y + elementBound.height / 2);
		assertEquals("Point does not match", spawnArray.getNextSpawnPoints(1, dynamicElements).getFirst(),
				new VPoint(first.x + 2 * 2 * elementBound.width / 2, first.y + 1 * 2 * elementBound.height / 2));
		// now at point 12 because getNextSpawnPoints() increments NextPointIndex

		// spawn 81 more to wrapp around to point 12.
		IntStream.range(0, 81).forEach(i -> spawnArray.getNextSpawnPoints(1, dynamicElements));
		assertEquals("Next Element does not match", 12, spawnArray.getNextSpawnPointIndex());
		assertEquals("Point does not match", spawnArray.getNextSpawnPoints(1, dynamicElements).getFirst(),
				new VPoint(first.x + 3 * 2 * elementBound.width / 2, first.y + 1 * 2 * elementBound.height / 2));

		VPoint[] spawnPoints = spawnArray.getSpawnPoints();
		List<DynamicElement> dynamicElements2 = createMock(0.5,
				spawnPoints[12],	// direct match (use next)
				spawnPoints[13].add(new VPoint(0, SpawnArray.OVERLAPP_EPSILON - 0.1)), // match within Epsilon (use next)
				spawnPoints[14].add(new VPoint(SpawnArray.OVERLAPP_EPSILON + 0.1, 0)) // match outside Epsilon (use this one)
		);
		assertEquals("Point does not match", spawnPoints[14], spawnArray.getNextSpawnPoints(1, dynamicElements2).getFirst());
		assertEquals("Next Element does not match", 15, spawnArray.getNextSpawnPointIndex());
	}

	// if all points are occupied measured only with centroid point throw exception
	@Test()
	public void PointsWithException(){
		source = new VRectangle(1.0, 1.0, 2.0, 2.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);

		VPoint[] spawnPoints = spawnArray.getSpawnPoints();
		List<DynamicElement> dynamicElements = createMock(0.5,
				spawnPoints[0],
				spawnPoints[1],
				spawnPoints[2],
				spawnPoints[3].add(new VPoint(0, 0.0003))
				);
		assertEquals("there should be no free spot", 0, spawnArray.getNextSpawnPoints(1, dynamicElements).size());

	}

	@Test
	public void testFreeSpawn() {
		source = new VRectangle(1.0, 1.0, 2.0, 2.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);

		assertEquals("Number of spawn points does not match", 4, spawnArray.getSpawnPoints().length);
		assertEquals("There should not be a free spot.", 0, spawnArray.getNextFreeSpawnPoints(1,
				createMock(0.5, spawnArray.getSpawnPoints())).size() );
		VPoint[] spawnPoints = spawnArray.getSpawnPoints();
		assertNotEquals("Point 1 is occupied and should not be returned", spawnPoints[1],
				spawnArray.getNextFreeSpawnPoints(1, createMock(0.5, spawnPoints[1])
		));
		assertNotNull("There should be three valid points",
				spawnArray.getNextFreeSpawnPoints( 1, createMock(0.5, spawnPoints[1])
		));

	}

	@Test
	public void testFreeSpawns() {
		source = new VRectangle(1.0, 1.0, 4.0, 4.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);

		assertEquals("Number of spawn points does not match", 16, spawnArray.getSpawnPoints().length);

		VPoint[] spawnPoints = spawnArray.getSpawnPoints();
		List<DynamicElement> dynamicElements = createMock(0.5,
				spawnPoints[0],
				spawnPoints[1],
				spawnPoints[3],
				spawnPoints[4]);
		LinkedList<VPoint> points = spawnArray.getNextFreeSpawnPoints(4, dynamicElements);
		assertEquals(4, points.size());
		assertEquals(spawnPoints[2], points.pollFirst());
		assertEquals(spawnPoints[5], points.pollFirst());
		assertEquals(spawnPoints[6], points.pollFirst());
		assertEquals(spawnPoints[7], points.pollFirst());

		source = new VRectangle(1.0, 1.0, 2.0, 2.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);
		assertEquals("Number of spawn points does not match", 4, spawnArray.getSpawnPoints().length);

		spawnPoints = spawnArray.getSpawnPoints();
		dynamicElements = createMock(0.5,
				spawnPoints[0],
				spawnPoints[1],
				spawnPoints[3]);
		points = spawnArray.getNextFreeSpawnPoints(4, dynamicElements);
		assertEquals(1, points.size());
		assertEquals(spawnPoints[2], points.pollFirst());

	}

	@Test
	public void testGroupSpawnMatchDims() {
		source = new VRectangle(1.0, 1.0, 8.0, 8.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);
		assertEquals("Number of spawn points does not match", 64, spawnArray.getSpawnPoints().length);

		VPoint[] spawnPoint = spawnArray.getSpawnPoints();
		List<DynamicElement> dynamicElements = createMock(0.5);

		//group 0
		LinkedList<VPoint> group = spawnArray.getNextGroup(4, dynamicElements);
		assertEquals( spawnPoint[0], group.pollFirst());
		assertEquals( spawnPoint[1], group.pollFirst());
		assertEquals( spawnPoint[8], group.pollFirst());
		assertEquals( spawnPoint[9], group.pollFirst());

		//group 1
		group = spawnArray.getNextGroup(4, dynamicElements);
		assertEquals( spawnPoint[1], group.pollFirst());
		assertEquals( spawnPoint[2], group.pollFirst());
		assertEquals( spawnPoint[9], group.pollFirst());
		assertEquals( spawnPoint[10], group.pollFirst());

		//group 2-3
		spawnArray.getNextGroup(4 , dynamicElements);
		group = spawnArray.getNextGroup(4, dynamicElements);
		assertEquals( spawnPoint[3], group.pollFirst());
		assertEquals( spawnPoint[4], group.pollFirst());
		assertEquals( spawnPoint[11], group.pollFirst());
		assertEquals( spawnPoint[12], group.pollFirst());

		//group 8 (line wrap)
		IntStream.range(4, 7).forEach(i -> spawnArray.getNextGroup(4,dynamicElements));
		group = spawnArray.getNextGroup(4, dynamicElements);
		assertEquals( spawnPoint[8], group.pollFirst());
		assertEquals( spawnPoint[9], group.pollFirst());
		assertEquals( spawnPoint[16], group.pollFirst());
		assertEquals( spawnPoint[17], group.pollFirst());

		//group 48 (last group)
		IntStream.range(8, 48).forEach(i -> spawnArray.getNextGroup(4, dynamicElements));
		group = spawnArray.getNextGroup(4, dynamicElements);
		assertEquals( spawnPoint[54], group.pollFirst());
		assertEquals( spawnPoint[55], group.pollFirst());
		assertEquals( spawnPoint[62], group.pollFirst());
		assertEquals( spawnPoint[63], group.pollFirst());

		// group 0 (wrap around to first group)
		group = spawnArray.getNextGroup(4, dynamicElements);
		assertEquals( spawnPoint[0], group.pollFirst());
		assertEquals( spawnPoint[1], group.pollFirst());
		assertEquals( spawnPoint[8], group.pollFirst());
		assertEquals( spawnPoint[9], group.pollFirst());

	}


	@Test
	public void testGroupSpawnNoMatchDims() {
		source = new VRectangle(1.0, 1.0, 8.0, 8.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);
		assertEquals("Number of spawn points does not match", 64, spawnArray.getSpawnPoints().length);

		VPoint[] spawnPoints = spawnArray.getSpawnPoints();
		List<DynamicElement> dynamicElements = createMock(0.5);

		//group 0
		LinkedList<VPoint> group = spawnArray.getNextGroup(6, dynamicElements);
		assertEquals(spawnPoints[0], group.pollFirst());
		assertEquals(spawnPoints[1], group.pollFirst());
		assertEquals(spawnPoints[2], group.pollFirst());
		assertEquals(spawnPoints[8], group.pollFirst());
		assertEquals(spawnPoints[9], group.pollFirst());
		assertEquals(spawnPoints[10], group.pollFirst());

		//group 1
		group = spawnArray.getNextGroup(6, dynamicElements);
		assertEquals(spawnPoints[1], group.pollFirst());
		assertEquals(spawnPoints[2], group.pollFirst());
		assertEquals(spawnPoints[3], group.pollFirst());
		assertEquals(spawnPoints[9], group.pollFirst());
		assertEquals(spawnPoints[10], group.pollFirst());
		assertEquals(spawnPoints[11], group.pollFirst());

		//group 6 (line wrap)
		IntStream.range(2, 6).forEach(i -> spawnArray.getNextGroup(6, dynamicElements));
		group = spawnArray.getNextGroup(6, dynamicElements);
		assertEquals(spawnPoints[8], group.pollFirst());
		assertEquals(spawnPoints[9], group.pollFirst());
		assertEquals(spawnPoints[10], group.pollFirst());
		assertEquals(spawnPoints[16], group.pollFirst());
		assertEquals(spawnPoints[17], group.pollFirst());
		assertEquals(spawnPoints[18], group.pollFirst());

		//group 41 (last group)
		IntStream.range(7, 41).forEach(i -> spawnArray.getNextGroup(6, dynamicElements));
		group = spawnArray.getNextGroup(6, dynamicElements);
		assertEquals(spawnPoints[53], group.pollFirst());
		assertEquals(spawnPoints[54], group.pollFirst());
		assertEquals(spawnPoints[55], group.pollFirst());
		assertEquals(spawnPoints[61], group.pollFirst());
		assertEquals(spawnPoints[62], group.pollFirst());
		assertEquals(spawnPoints[63], group.pollFirst());

		//group 0 (wrap around to first group)
		group = spawnArray.getNextGroup(6, dynamicElements);
		assertEquals(spawnPoints[0], group.pollFirst());
		assertEquals(spawnPoints[1], group.pollFirst());
		assertEquals(spawnPoints[2], group.pollFirst());
		assertEquals(spawnPoints[8], group.pollFirst());
		assertEquals(spawnPoints[9], group.pollFirst());
		assertEquals(spawnPoints[10], group.pollFirst());

		// In this case allow Overlapping but make sure the centroid of the new group members
		// do not directly overlap with each other.
		List<DynamicElement> dynamicElements2 = createMock(0.5,
				spawnPoints[1],	// direct match (use next)
				spawnPoints[2].add(new VPoint(SpawnArray.OVERLAPP_EPSILON - 0.1, 0)), // match within Epsilon (use next)
				spawnPoints[3].add(new VPoint(SpawnArray.OVERLAPP_EPSILON + 0.1, SpawnArray.OVERLAPP_EPSILON + 0.1)) // match outside Epsilon (use this one)
		);
		group = spawnArray.getNextGroup(6, dynamicElements2);
		assertEquals(spawnPoints[3], group.pollFirst());
		assertEquals(spawnPoints[4], group.pollFirst());
		assertEquals(spawnPoints[5], group.pollFirst());
		assertEquals(spawnPoints[11], group.pollFirst());
		assertEquals(spawnPoints[12], group.pollFirst());
		assertEquals(spawnPoints[13], group.pollFirst());
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void mixedGroupWithErr() {
		source = new VRectangle(1.0, 1.0, 8.0, 8.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);
		assertEquals("Number of spawn points does not match", 64, spawnArray.getSpawnPoints().length);

		VPoint[] spawnPoint = spawnArray.getSpawnPoints();
		List<DynamicElement> dynamicElements = createMock(0.5);

		//group 3 lines
		LinkedList<VPoint> group = spawnArray.getNextGroup(9, dynamicElements);
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[2]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);
		assertEquals(group.pollFirst(), spawnPoint[10]);
		assertEquals(group.pollFirst(), spawnPoint[16]);
		assertEquals(group.pollFirst(), spawnPoint[17]);
		assertEquals(group.pollFirst(), spawnPoint[18]);

		//group 0
		group = spawnArray.getNextGroup(4, dynamicElements);
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);

		// spawning different size groups does not effect other groups
		spawnArray.getNextGroup(9, dynamicElements);
		spawnArray.getNextGroup(9, dynamicElements);

		//group 1
		group = spawnArray.getNextGroup(4, dynamicElements);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[2]);
		assertEquals(group.pollFirst(), spawnPoint[9]);
		assertEquals(group.pollFirst(), spawnPoint[10]);

		spawnArray.getNextGroup(100, dynamicElements);

	}

	@Test
	public void testFreeGroupSpawn() {
		source = new VRectangle(1.0, 1.0, 8.0, 8.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);
		assertEquals("Number of spawn points does not match", 64, spawnArray.getSpawnPoints().length);

		VPoint[] spawnPoint = spawnArray.getSpawnPoints();
		LinkedList<VPoint> group = spawnArray.getNextFreeGroup(6, createMock(0.5, new VPoint(99.0, 99.0)));
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[2]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);
		assertEquals(group.pollFirst(), spawnPoint[10]);

		// empty neighbours
		group = spawnArray.getNextFreeGroup(6, new LinkedList<DynamicElement>());
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[2]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);
		assertEquals(group.pollFirst(), spawnPoint[10]);


		// match group 3 (with overlapping groups)
		List<DynamicElement> dynamicElements = createMock(0.5, spawnPoint[0], spawnPoint[1], spawnPoint[10]);
		group = spawnArray.getNextFreeGroup(6, dynamicElements);
		assertEquals(group.pollFirst(), spawnPoint[3]);
		assertEquals(group.pollFirst(), spawnPoint[4]);
		assertEquals(group.pollFirst(), spawnPoint[5]);
		assertEquals(group.pollFirst(), spawnPoint[11]);
		assertEquals(group.pollFirst(), spawnPoint[12]);
		assertEquals(group.pollFirst(), spawnPoint[13]);

		//match group 9 (with overlapping groups and line wrap)
		dynamicElements = createMock(0.5,
				spawnPoint[0],
				spawnPoint[1],
				spawnPoint[2],
				spawnPoint[3],
				spawnPoint[6],
				spawnPoint[9]);
		group = spawnArray.getNextFreeGroup(6, dynamicElements);
		assertEquals(group.pollFirst(), spawnPoint[10]);
		assertEquals(group.pollFirst(), spawnPoint[11]);
		assertEquals(group.pollFirst(), spawnPoint[12]);
		assertEquals(group.pollFirst(), spawnPoint[18]);
		assertEquals(group.pollFirst(), spawnPoint[19]);
		assertEquals(group.pollFirst(), spawnPoint[20]);

	}

	private List<DynamicElement> createMock(double r, ArrayList<VPoint> points) {
		return createMock(r, points.toArray(new VPoint[points.size()]));
	}

	private List<DynamicElement> createMock(double r, VPoint... points) {
		LinkedList<DynamicElement> elements = new LinkedList<>();

		for (VPoint p : points) {
			VCircle c = new VCircle(p, r);
			DynamicElement e = Mockito.mock(DynamicElement.class, Mockito.RETURNS_DEEP_STUBS);
			Mockito.when(e.getShape()).thenReturn(c);
			elements.add(e);
		}
		return elements;
	}

	class TestAppender extends AppenderSkeleton {
		private final List<LoggingEvent> log = new ArrayList<LoggingEvent>();


		@Override
		protected void append(LoggingEvent loggingEvent) {
			log.add(loggingEvent);
		}

		@Override
		public boolean requiresLayout() {
			return false;
		}

		@Override
		public void close() {

		}

		public List<LoggingEvent> getLog() {
			return new ArrayList<LoggingEvent>(log);
		}
	}
}