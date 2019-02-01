package org.vadere.util.geometry;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Thorough test of the {@link LinkedCellsGrid}.
 * 
 * 
 */
public class TestLinkedCellsGrid {
	private static Logger logger = Logger
			.getLogger(TestLinkedCellsGrid.class);

	private class NotComparableObject implements PointPositioned {
		public int value;
		public VPoint coord;

		public NotComparableObject(int value, VPoint coord) {
			this.value = value;
			this.coord = coord;
		}

		@Override
		public VPoint getPosition() {
			return coord;
		}
	}

	private static class CoordinatedInteger implements PointPositioned {
		public final Integer number;
		public final VPoint coordinate;

		public CoordinatedInteger(Integer number, VPoint coordinate) {
			this.number = number;
			this.coordinate = coordinate;
		}


		@Override
		public VPoint getPosition() {
			return coordinate;
		}
	}

	private static final double left = 0;
	private static final double top = 0;
	private static final double width = 100;
	private static final double height = 100;
	private static final double sideLength = 1;

	VPoint pos1 = new VPoint(0, 0);
	VPoint pos2 = new VPoint(10, 10);
	VPoint pos3 = new VPoint(50, 10);

	int int1 = 1;
	int int2 = 2;
	int int3 = 3;
	int int4 = 4;

	NotComparableObject obj1 = new NotComparableObject(1, pos1);
	NotComparableObject obj2 = new NotComparableObject(2, pos2);
	NotComparableObject obj3 = new NotComparableObject(3, pos3);
	NotComparableObject obj4 = new NotComparableObject(4, pos3);

	/** linked cells grid with comparable objects */
	private static LinkedCellsGrid<CoordinatedInteger> linkedCellsInteger;
	/** linked cells grid with non comparable objects */
	private static LinkedCellsGrid<NotComparableObject> linkedCellsObject;

	/**
	 * Initializes the linked cells grids so that each test gets a clean object.
	 * 
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		linkedCellsInteger = new LinkedCellsGrid<CoordinatedInteger>(left, top, width,
				height, sideLength);
		linkedCellsObject = new LinkedCellsGrid<NotComparableObject>(left, top,
				width, height, sideLength);
	}

	/**
	 * Test method for
	 * {@link org.vadere.util.geometry.LinkedCellsGrid#addObject(PointPositioned)}
	 * . adds several integer objects and tries to retrieve them via
	 * {@link LinkedCellsGrid#getObjects(VPoint, double)}
	 * .
	 */
	@Test
	public void testAddObject() {
		CoordinatedInteger coordinatedInteger1 = new CoordinatedInteger(int1, pos1);
		CoordinatedInteger coordinatedInteger2 = new CoordinatedInteger(int2, pos2);
		CoordinatedInteger coordinatedInteger3 = new CoordinatedInteger(int3, pos3);
		linkedCellsInteger.addObject(coordinatedInteger1);
		linkedCellsInteger.addObject(coordinatedInteger2);
		linkedCellsInteger.addObject(coordinatedInteger3);

		// the values are chosen so that all points should clearly be inside the
		// ball
		VPoint testpos1 = new VPoint(25, 25);
		double testradius1 = 40;
		List<CoordinatedInteger> objects1 = linkedCellsInteger.getObjects(testpos1, testradius1);

		assertEquals("the grid did not add the correct number of objects.", 3,
				objects1.size());
		assertTrue("the first object was not added correctly",
				objects1.contains(coordinatedInteger1));
		assertTrue("the second object was not added correctly",
				objects1.contains(coordinatedInteger2));
		assertTrue("the third object was not added correctly",
				objects1.contains(coordinatedInteger3));

		// add a new object at exactly the same position as before. should NOT
		// be added
		linkedCellsInteger.addObject(new CoordinatedInteger(int4, pos3));

		List<CoordinatedInteger> objects2 = linkedCellsInteger.getObjects(testpos1, testradius1);
		assertEquals("the grid did not add the object but should have.", 4,
				objects2.size());

		// add exactly the same object at exactly the same position as before.
		// should also be added
		linkedCellsInteger.addObject(new CoordinatedInteger(int1, pos1));

		List<CoordinatedInteger> objects3 = linkedCellsInteger.getObjects(testpos1, testradius1);
		assertEquals("the grid did add the object but should not have.", 5,
				objects3.size());
	}

	/**
	 * Test method for
	 * {@link org.vadere.util.geometry.LinkedCellsGrid#addObject(PointPositioned)}
	 * . adds several non comparable objects and tries to retrieve them via
	 * {@link LinkedCellsGrid#getObjects(VPoint, double)}
	 * .
	 */
	@Test
	public void testAddNonComparableObject() {
		linkedCellsObject.addObject(obj1);
		linkedCellsObject.addObject(obj2);
		linkedCellsObject.addObject(obj3);

		// the values are chosen so that all points should clearly be inside the
		// ball
		VPoint testpos1 = new VPoint(25, 25);
		double testradius1 = 40;
		List<NotComparableObject> objects1 = linkedCellsObject.getObjects(
				testpos1, testradius1);

		assertEquals("the grid did not add the correct number of objects.", 3,
				objects1.size());
		assertTrue("the first object was not added correctly",
				objects1.contains(obj1));
		assertTrue("the second object was not added correctly",
				objects1.contains(obj2));
		assertTrue("the third object was not added correctly",
				objects1.contains(obj3));

		// add a new object at exactly the same position as before. should be
		// added
		linkedCellsObject.addObject(obj4);

		List<NotComparableObject> objects2 = linkedCellsObject.getObjects(
				testpos1, testradius1);
		assertEquals("the grid did not add the object but should have.", 4,
				objects2.size());

		// add exactly the same object at exactly the same position as before.
		// should also be added
		linkedCellsObject.addObject(obj1);

		List<NotComparableObject> objects3 = linkedCellsObject.getObjects(
				testpos1, testradius1);
		assertEquals("the grid did add the object but should not have.", 5,
				objects3.size());
	}

	/**
	 * Test method for
	 * {@link org.vadere.util.geometry.LinkedCellsGrid#addObject(PointPositioned)}
	 * . adds several points and tests the number of objects stored in the
	 * linked cells grid after adding three comparable objects.
	 */
	@Test
	public void testAddObjectSize() {
		linkedCellsInteger.addObject(new CoordinatedInteger(int1, pos1));
		linkedCellsInteger.addObject(new CoordinatedInteger(int2, pos2));
		linkedCellsInteger.addObject(new CoordinatedInteger(int3, pos3));

		assertEquals("size of linkedCellsDouble is wrong", 3,
				linkedCellsInteger.size());

		// add exactly the same object at exactly the same position as before.
		// should still be added
		linkedCellsInteger.addObject(new CoordinatedInteger(int1, pos3));

		assertEquals("size of linkedCellsDouble is wrong", 4,
				linkedCellsInteger.size());
	}

	/**
	 * Test method for
	 * {@link org.vadere.util.geometry.LinkedCellsGrid#getObjects(VPoint, double)}
	 * . Adds objects and tries to retrieve them via getObjects.
	 */
	@Test
	public void testGetObjects() {
		linkedCellsInteger.addObject(new CoordinatedInteger(int1, pos1));
		linkedCellsInteger.addObject(new CoordinatedInteger(int2, pos2));
		linkedCellsInteger.addObject(new CoordinatedInteger(int3, pos3));

		// this should only return the object at pos1
		List<CoordinatedInteger> objects = linkedCellsInteger.getObjects(pos1,
				pos1.distance(pos2) - 1);
		assertEquals(
				"getObjects did not return the correct number of objects.", 1,
				objects.size());
		assertEquals("getObjects did not return the correct object.", int1,
				(int) objects.get(0).number);

		// this should return the object at pos1 and pos2
		List<CoordinatedInteger> objects2 = linkedCellsInteger.getObjects(pos1,
				pos1.distance(pos2) + 1);
		assertEquals(
				"getObjects did not return the correct number of objects.", 2,
				objects2.size());
		assertEquals("getObjects did not return the correct object.", int1,
				(int) objects2.get(0).number);
		assertEquals("getObjects did not return the correct object.", int2,
				(int) objects2.get(1).number);
	}

	/**
	 * Test method for
	 * {@link org.vadere.util.geometry.LinkedCellsGrid#removeObject(PointPositioned)}
	 * objects and tries to remove one.
	 */
	@Test
	public void testRemoveObject() {
		CoordinatedInteger coordinatedInteger = new CoordinatedInteger(int2, pos2);
		linkedCellsInteger.addObject(new CoordinatedInteger(int1, pos1));
		linkedCellsInteger.addObject(coordinatedInteger);

		// remove the second object
		linkedCellsInteger.removeObject(coordinatedInteger);
		assertEquals("remove object did not remove the object.", 1,
				linkedCellsInteger.size());
		assertEquals("remove object did not remove the last object.", int1,
				(int) linkedCellsInteger.iterator().next().number);
	}

	/**
	 * Test method for {@link org.vadere.util.geometry.LinkedCellsGrid#clear()}. Adds two
	 * objects, clears the grid and checks if it is really empty.
	 */
	@Test
	public void testClear() {
		linkedCellsInteger.addObject(new CoordinatedInteger(int1, pos1));
		linkedCellsInteger.addObject(new CoordinatedInteger(int2, pos2));

		linkedCellsInteger.clear();

		assertEquals("the grid is not empty.", 0, linkedCellsInteger.size());

		// try to retrieve the objects, should return an empty list
		List<CoordinatedInteger> objects = linkedCellsInteger.getObjects(pos1,
				pos1.distance(pos2) + 1);
		assertEquals("there are still objects present in the grid.", 0,
				objects.size());
	}

	/**
	 * Test method for {@link org.vadere.util.geometry.LinkedCellsGrid#iterator()}. Adds three
	 * objects and iterates over them.
	 */
	@Test
	public void testIterator() {
		linkedCellsInteger.addObject(new CoordinatedInteger(int1, pos1));
		linkedCellsInteger.addObject(new CoordinatedInteger(int2, pos2));
		linkedCellsInteger.addObject(new CoordinatedInteger(int3, pos3));

		List<Integer> objects = new LinkedList<Integer>();

		Iterator<CoordinatedInteger> objectIterator = linkedCellsInteger.iterator();
		assertEquals("the iterator should find the first element", true,
				objectIterator.hasNext());
		objects.add(objectIterator.next().number);
		assertEquals("the iterator should find the second element", true,
				objectIterator.hasNext());
		objects.add(objectIterator.next().number);
		assertEquals("the iterator should find the third element", true,
				objectIterator.hasNext());
		objects.add(objectIterator.next().number);
		assertEquals(
				"the iterator should not find any elements after the third element",
				false, objectIterator.hasNext());

		// check if the correct objects were returned
		assertTrue("the iterator did not return object 1",
				objects.contains(int1));
		assertTrue("the iterator did not return object 2",
				objects.contains(int2));
		assertTrue("the iterator did not return object 3",
				objects.contains(int3));
	}

	/**
	 * Test method for {@link org.vadere.util.geometry.LinkedCellsGrid#size()}. Adds three
	 * objects, removes one object, clears the grid and checks the size before
	 * and after each operation.
	 */
	@Test
	public void testSize() {
		assertEquals("grid is not empty.", 0, linkedCellsInteger.size());
		CoordinatedInteger coordinatedInteger = new CoordinatedInteger(int1, pos1);
		linkedCellsInteger.addObject(coordinatedInteger);
		linkedCellsInteger.addObject(new CoordinatedInteger(int2, pos2));
		linkedCellsInteger.addObject(new CoordinatedInteger(int3, pos3));

		assertEquals("grid does not contain the correct number of objects.", 3,
				linkedCellsInteger.size());

		linkedCellsInteger.removeObject(coordinatedInteger);

		assertEquals("grid does not contain the correct number of objects.", 2,
				linkedCellsInteger.size());

		linkedCellsInteger.clear();

		assertEquals("grid is not empty after clear.", 0,
				linkedCellsInteger.size());
	}

	/**
	 * Test method for
	 * {@link org.vadere.util.geometry.LinkedCellsGrid#contains(PointPositioned)}. Checks
	 * contains on an empty grid as well as with three added objects.
	 */
	@Test
	public void testContainsT() {
		CoordinatedInteger coordinatedInteger1 = new CoordinatedInteger(int1, pos1);
		CoordinatedInteger coordinatedInteger2 = new CoordinatedInteger(int2, pos2);
		CoordinatedInteger coordinatedInteger3 = new CoordinatedInteger(int3, pos3);
		CoordinatedInteger coordinatedInteger4 = new CoordinatedInteger(int4, pos3);
		assertFalse("grid did contain object 1 even it was empty.",
				linkedCellsInteger.contains(coordinatedInteger1));



		linkedCellsInteger.addObject(coordinatedInteger1);
		linkedCellsInteger.addObject(coordinatedInteger2);
		linkedCellsInteger.addObject(coordinatedInteger3);

		assertTrue("grid did not contain object 1.",
				linkedCellsInteger.contains(coordinatedInteger1));
		assertTrue("grid did not contain object 1.",
				linkedCellsInteger.contains(coordinatedInteger2));
		assertTrue("grid did not contain object 1.",
				linkedCellsInteger.contains(coordinatedInteger3));
		assertFalse("grid did contain object 4.",
				linkedCellsInteger.contains(coordinatedInteger4));
	}

	/**
	 * Test method for the complexity of
	 * {@link LinkedCellsGrid#getObjects(VPoint, double)}. Should be O(1).
	 */
	@Test
	public void testGetObjectsCompexity() {
		// throw a lot of objects in the grid, equally spaced.
		int[] objCounts = new int[] {10, 100, 1000, 10000, 100000};

		List<Long> times = new LinkedList<Long>();

		for (int count : objCounts) {
			int numberOfObjects = count;

			// create a grid that holds at max one object per cell
			double sideLength = width / Math.sqrt(count);
			linkedCellsInteger = new LinkedCellsGrid<>(left, top, width,
					height, sideLength);
			linkedCellsInteger.clear();
			fillGrid(linkedCellsInteger, numberOfObjects);

			// access the grid a lot of times, compute the time needed
			int numberOfSearches = (int) 1e6;
			VPoint searchPos = new VPoint(width / 2, height / 2);
			double radius = sideLength * 2;
			long startTime = System.currentTimeMillis();

			for (int search = 0; search < numberOfSearches; search++) {
				linkedCellsInteger.getObjects(searchPos, radius);
			}

			long totalTime = System.currentTimeMillis() - startTime;

			times.add(totalTime);
			logger.debug(String.format(
					"searching %d objects %d times took %d ms.", count,
					numberOfSearches, totalTime));
		}

		// test whether the times are similar
		double mean = mean(times);
		logger.debug(String.format("mean of all search times: %.2f ms", mean));
		for (int time = 1; time < times.size(); time++) {
			assertTrue("getObjects took too much time.",
					times.get(time) < mean * 3);
		}
	}

	private double mean(List<Long> times) {
		double mean = 0.0;
		for (Long time : times) {
			mean += time;
		}
		return mean / times.size();
	}

	/**
	 * Fills an integer grid with a given number of objects.
	 * 
	 * @param linkedCellsGrid
	 * @param numberOfObjects
	 */
	private void fillGrid(LinkedCellsGrid<CoordinatedInteger> linkedCellsGrid,
			int numberOfObjects) {
		int size = (int) Math.sqrt(numberOfObjects);
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				int obj = row * size + col;
				VPoint pos = new VPoint(row / (double) size * height, col
						/ (double) size * width);
				linkedCellsGrid.addObject(new CoordinatedInteger(obj, pos));
			}
		}
	}

}
