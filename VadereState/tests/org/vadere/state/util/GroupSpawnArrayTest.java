package org.vadere.state.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class GroupSpawnArrayTest {

	private VRectangle source;
	private VRectangle elementBound;
	private GroupSpawnArray spawnArray;
	private AttributesAgent attr = new AttributesAgent();

	private VShape shapeProducer(VPoint vPoint){
		return new VCircle(vPoint, attr.getRadius());
	}

	VPoint[] spawnPointsAsArray(SpawnArray spawnArray){
		int len = spawnArray.getAllowedSpawnPoints().size();
		return spawnArray.getAllowedSpawnPoints().toArray(new VPoint[len]);
	}

	@Test
	public void testGroupSpawnMatchDims() {
		source = new VRectangle(1.0, 1.0, 8.0, 8.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new GroupSpawnArray(source, elementBound, this::shapeProducer);
		assertEquals("Number of spawn points does not match", 64, spawnArray.getAllowedSpawnPoints().size());

		VPoint[] spawnPoint = spawnPointsAsArray(spawnArray);
		List<VShape> dynamicElements = createMock();

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
		spawnArray = new GroupSpawnArray(source, elementBound, this::shapeProducer);
		assertEquals("Number of spawn points does not match", 64, spawnArray.getAllowedSpawnPoints().size());

		VPoint[] spawnPoints = spawnPointsAsArray(spawnArray);
		List<VShape> dynamicElements = createMock();

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
		List<VShape> dynamicElements2 = createMock(  //todo
				spawnPoints[1],	// direct match (use next)
				spawnPoints[2].add(new VPoint( 0.1, 0)), // match within Epsilon (use next)
				spawnPoints[3].add(new VPoint(0.1, 0.1)) // match outside Epsilon (use this one)
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
		spawnArray = new GroupSpawnArray(source, elementBound, this::shapeProducer);
		assertEquals("Number of spawn points does not match", 64, spawnArray.getAllowedSpawnPoints().size());

		VPoint[] spawnPoint = spawnPointsAsArray(spawnArray);
		List<VShape> dynamicElements = createMock();

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
		spawnArray = new GroupSpawnArray(source, elementBound, this::shapeProducer);
		assertEquals("Number of spawn points does not match", 64, spawnArray.getAllowedSpawnPoints().size());

		VPoint[] spawnPoint = spawnPointsAsArray(spawnArray);
		LinkedList<VPoint> group = spawnArray.getNextFreeGroup(6, createMock( new VPoint(99.0, 99.0)));
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[2]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);
		assertEquals(group.pollFirst(), spawnPoint[10]);

		// empty neighbours
		group = spawnArray.getNextFreeGroup(6, new LinkedList<>());
		assertEquals(group.pollFirst(), spawnPoint[0]);
		assertEquals(group.pollFirst(), spawnPoint[1]);
		assertEquals(group.pollFirst(), spawnPoint[2]);
		assertEquals(group.pollFirst(), spawnPoint[8]);
		assertEquals(group.pollFirst(), spawnPoint[9]);
		assertEquals(group.pollFirst(), spawnPoint[10]);


		// match group 3 (with overlapping groups)
		List<VShape> dynamicElements = createMock( spawnPoint[0], spawnPoint[1], spawnPoint[10]);
		group = spawnArray.getNextFreeGroup(6, dynamicElements);
		assertEquals(group.pollFirst(), spawnPoint[3]);
		assertEquals(group.pollFirst(), spawnPoint[4]);
		assertEquals(group.pollFirst(), spawnPoint[5]);
		assertEquals(group.pollFirst(), spawnPoint[11]);
		assertEquals(group.pollFirst(), spawnPoint[12]);
		assertEquals(group.pollFirst(), spawnPoint[13]);

		//match group 9 (with overlapping groups and line wrap)
		dynamicElements = createMock(
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

	/**
	 * Group dimension is set as W x H for a Group of 6 this would be 3x2 Test if Source Width is
	 * smaller than 3! Here the Group must be 2x3
	 */
	@Test
	public void groupXDimBiggerThanSource() {
		source = new VRectangle(1.0, 1.0, 2.0, 7);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new GroupSpawnArray(source, elementBound, this::shapeProducer);

		VPoint[] spawnPoints = spawnPointsAsArray(spawnArray);
		LinkedList<VPoint> points = spawnArray.getNextGroup(6, createMock());
		assertEquals(spawnPoints[0], points.pollFirst());
		assertEquals(spawnPoints[1], points.pollFirst());
		assertEquals(spawnPoints[2], points.pollFirst());
		assertEquals(spawnPoints[3], points.pollFirst());
		assertEquals(spawnPoints[4], points.pollFirst());
		assertEquals(spawnPoints[5], points.pollFirst());

	}


	private List<VShape> createMock( List<VPoint> points) {
		return  points.stream().map(this::shapeProducer).collect(Collectors.toList());
	}

	private List<VShape> createMock( VPoint... points) {
		return  createMock(Arrays.asList(points));
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