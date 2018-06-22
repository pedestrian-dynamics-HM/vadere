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

	VRectangle source;
	VRectangle elementBound;
	SpawnArray spawnArray;

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
		LinkedList<VPoint> points = spawnArray.getNextGroup(6);
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
		VPoint p = spawnArray.getNextSpawnPoint();
		assertEquals("Point does not match", p, new VPoint(1.5, 1.5));
		assertEquals("Next Element does not match", 1, spawnArray.getNextSpawnPointIndex());

		// 10 more points
		IntStream.range(0, 10).forEach(i -> spawnArray.getNextSpawnPoint());
		assertEquals("Next Element does not match", 11, spawnArray.getNextSpawnPointIndex());
		VPoint first = new VPoint(source.x + elementBound.width / 2, source.y + elementBound.height / 2);
		assertEquals("Point does not match", spawnArray.getNextSpawnPoint(),
				new VPoint(first.x + 2 * 2 * elementBound.width / 2, first.y + 1 * 2 * elementBound.height / 2));
		// now at point 12 because getNextSpawnPoint() increments NextPointIndex

		// spawn 81 more to wrapp around to point 12.
		IntStream.range(0, 81).forEach(i -> spawnArray.getNextSpawnPoint());
		assertEquals("Next Element does not match", 12, spawnArray.getNextSpawnPointIndex());
		assertEquals("Point does not match", spawnArray.getNextSpawnPoint(),
				new VPoint(first.x + 3 * 2 * elementBound.width / 2, first.y + 1 * 2 * elementBound.height / 2));
	}

	@Test
	public void testFreeSpawn() {
		source = new VRectangle(1.0, 1.0, 2.0, 2.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);

		assertEquals("Number of spawn points does not match", 4, spawnArray.getSpawnPoints().length);
		assertNull("There should not be a free spot.", spawnArray.getNextFreeSpawnPoint(
				createMock(0.5, spawnArray.getSpawnPoints())));
		VPoint[] spawnPoints = spawnArray.getSpawnPoints();
		assertNotEquals("Point 1 is occupied and should not be returned", spawnPoints[1], spawnArray.getNextFreeSpawnPoint(
				createMock(0.5, spawnPoints[1])
		));
		assertNotNull("There should be three valid points", spawnArray.getNextFreeSpawnPoint(
				createMock(0.5, spawnPoints[1])
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

		//group 0
		LinkedList<VPoint> group = spawnArray.getNextGroup(4);
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);

		//group 1
		group = spawnArray.getNextGroup(4);
		assertEquals(group.pollFirst(), spawnPoint[2]);
		assertEquals(group.pollFirst(), spawnPoint[3]);
		assertEquals(group.pollFirst(), spawnPoint[10]);
		assertEquals(group.pollFirst(), spawnPoint[11]);

		//group 2-3
		spawnArray.getNextGroup(4);
		group = spawnArray.getNextGroup(4);
		assertEquals(group.pollFirst(), spawnPoint[6]);
		assertEquals(group.pollFirst(), spawnPoint[7]);
		assertEquals(group.pollFirst(), spawnPoint[14]);
		assertEquals(group.pollFirst(), spawnPoint[15]);

		//group 4 (line wrap)
		group = spawnArray.getNextGroup(4);
		assertEquals(group.pollFirst(), spawnPoint[16]);
		assertEquals(group.pollFirst(), spawnPoint[17]);
		assertEquals(group.pollFirst(), spawnPoint[24]);
		assertEquals(group.pollFirst(), spawnPoint[25]);

		//group 15 (last group)
		IntStream.range(0, 10).forEach(i -> spawnArray.getNextGroup(4));
		group = spawnArray.getNextGroup(4);
		assertEquals(group.pollFirst(), spawnPoint[54]);
		assertEquals(group.pollFirst(), spawnPoint[55]);
		assertEquals(group.pollFirst(), spawnPoint[62]);
		assertEquals(group.pollFirst(), spawnPoint[63]);

		// group 0 (wrap around to first group)
		group = spawnArray.getNextGroup(4);
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);

	}


	@Test
	public void testGroupSpawnNoMatchDims() {
		source = new VRectangle(1.0, 1.0, 8.0, 8.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);
		assertEquals("Number of spawn points does not match", 64, spawnArray.getSpawnPoints().length);

		VPoint[] spawnPoint = spawnArray.getSpawnPoints();

		//group 0
		LinkedList<VPoint> group = spawnArray.getNextGroup(6);
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[2]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);
		assertEquals(group.pollFirst(), spawnPoint[10]);

		//group 1
		group = spawnArray.getNextGroup(6);
		assertEquals(group.pollFirst(), spawnPoint[3]);
		assertEquals(group.pollFirst(), spawnPoint[4]);
		assertEquals(group.pollFirst(), spawnPoint[5]);
		assertEquals(group.pollFirst(), spawnPoint[11]);
		assertEquals(group.pollFirst(), spawnPoint[12]);
		assertEquals(group.pollFirst(), spawnPoint[13]);

		//group 2 (line wrap)
		group = spawnArray.getNextGroup(6);
		assertEquals(group.pollFirst(), spawnPoint[16]);
		assertEquals(group.pollFirst(), spawnPoint[17]);
		assertEquals(group.pollFirst(), spawnPoint[18]);
		assertEquals(group.pollFirst(), spawnPoint[24]);
		assertEquals(group.pollFirst(), spawnPoint[25]);
		assertEquals(group.pollFirst(), spawnPoint[26]);

		//group 7 (last group)
		IntStream.range(0, 4).forEach(i -> spawnArray.getNextGroup(6));
		group = spawnArray.getNextGroup(6);
		assertEquals(group.pollFirst(), spawnPoint[51]);
		assertEquals(group.pollFirst(), spawnPoint[52]);
		assertEquals(group.pollFirst(), spawnPoint[53]);
		assertEquals(group.pollFirst(), spawnPoint[59]);
		assertEquals(group.pollFirst(), spawnPoint[60]);
		assertEquals(group.pollFirst(), spawnPoint[61]);

		//group 0 (wrap around to first group)
		group = spawnArray.getNextGroup(6);
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[2]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);
		assertEquals(group.pollFirst(), spawnPoint[10]);

	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void mixedGroupWithErr() {
		source = new VRectangle(1.0, 1.0, 8.0, 8.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound);
		assertEquals("Number of spawn points does not match", 64, spawnArray.getSpawnPoints().length);

		VPoint[] spawnPoint = spawnArray.getSpawnPoints();

		//group 3 lines
		LinkedList<VPoint> group = spawnArray.getNextGroup(9);
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
		group = spawnArray.getNextGroup(4);
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);

		// spawning different size groups does not effect other groups
		spawnArray.getNextGroup(9);
		spawnArray.getNextGroup(9);

		//group 1
		group = spawnArray.getNextGroup(4);
		assertEquals(group.pollFirst(), spawnPoint[2]);
		assertEquals(group.pollFirst(), spawnPoint[3]);
		assertEquals(group.pollFirst(), spawnPoint[10]);
		assertEquals(group.pollFirst(), spawnPoint[11]);

		spawnArray.getNextGroup(100);

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