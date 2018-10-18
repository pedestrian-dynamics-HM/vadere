package org.vadere.state.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElement;
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
import static org.junit.Assert.assertNotNull;

public class SpawnArrayTest {

	private VRectangle source;
	private VRectangle elementBound;
	private SpawnArray spawnArray;
	private AttributesAgent attr = new AttributesAgent();

	private VShape shapeProducer(VPoint vPoint){
		return new VCircle(vPoint, attr.getRadius());
	}

	VPoint[] spawnPointsAsArray(SpawnArray spawnArray){
		int len = spawnArray.getAllowedSpawnPoints().size();
		return spawnArray.getAllowedSpawnPoints().toArray(new VPoint[len]);
	}

	@Test
	public void NumberOfElements() {
		// Number of Elements
		source = new VRectangle(1.0, 1.0, 9.9, 9.9);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound, this::shapeProducer);
		assertEquals("expected spawn points don't match", 81, spawnArray.getAllowedSpawnPoints().size());
	}

	@Test
	public void toSmallSource1() {
		// Number of Elements
		TestAppender testAppender = new TestAppender();
		Logger log = LogManager.getLogger(SpawnArray.class);
		log.addAppender(testAppender);

		source = new VRectangle(1.0, 1.0, 0.1, 0.1);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound, this::shapeProducer);
		assertEquals("expected spawn points don't match", 1, spawnArray.getAllowedSpawnPoints().size());

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
		spawnArray = new SpawnArray(source, elementBound, this::shapeProducer);
		assertEquals("expected spawn points don't match", 3, spawnArray.getAllowedSpawnPoints().size());
		String msg = String.format("Dimension of Source is to small for at least one" +
						" dimension to contain designated spawnElement with" +
						" Bound (%.2f x %.2f) Set to (%d x %d)",
				1.0, 1.0, 3, 1);
		assertEquals("", testAppender.getLog().get(0).getMessage(), msg);
		log.removeAppender(testAppender);
		log.removeAppender(testAppender);
	}


	@Test
	public void Points() {
		source = new VRectangle(1.0, 1.0, 9.9, 9.9);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound, this::shapeProducer);

		// first Point
		List<VShape> dynamicElements = createMock();
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

		VPoint[] spawnPoints = spawnPointsAsArray(spawnArray);
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
		spawnArray = new SpawnArray(source, elementBound, this::shapeProducer);

		VPoint[] spawnPoints = spawnPointsAsArray(spawnArray);
		List<VShape> dynamicElements = createMock(
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
		spawnArray = new SpawnArray(source, elementBound, this::shapeProducer);

		assertEquals("Number of spawn points does not match", 4, spawnArray.getAllowedSpawnPoints().size());
		assertEquals("There should not be a free spot.", 0, spawnArray.getNextFreeSpawnPoints(1,
				createMock(0.5, spawnArray.getSpawnPoints())).size() );
		VPoint[] spawnPoints = spawnPointsAsArray(spawnArray);
		assertNotEquals("Point 1 is occupied and should not be returned", spawnPoints[1],
				spawnArray.getNextFreeSpawnPoints(1, createMock( spawnPoints[1])
		));
		assertNotNull("There should be three valid points",
				spawnArray.getNextFreeSpawnPoints( 1, createMock( spawnPoints[1])
		));

	}

	@Test
	public void testFreeSpawns() {
		source = new VRectangle(1.0, 1.0, 4.0, 4.0);
		elementBound = new VRectangle(0.0, 0.0, 1.0, 1.0);
		spawnArray = new SpawnArray(source, elementBound, this::shapeProducer);

		assertEquals("Number of spawn points does not match", 16, spawnArray.getAllowedSpawnPoints().size());

		VPoint[] spawnPoints = spawnPointsAsArray(spawnArray);
		List<VShape> dynamicElements = createMock(
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
		spawnArray = new SpawnArray(source, elementBound, this::shapeProducer);
		assertEquals("Number of spawn points does not match", 4, spawnArray.getAllowedSpawnPoints().size());

		spawnPoints = spawnPointsAsArray(spawnArray);
		dynamicElements = createMock(
				spawnPoints[0],
				spawnPoints[1],
				spawnPoints[3]);
		points = spawnArray.getNextFreeSpawnPoints(4, dynamicElements);
		assertEquals(1, points.size());
		assertEquals(spawnPoints[2], points.pollFirst());

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