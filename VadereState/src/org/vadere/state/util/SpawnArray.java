package org.vadere.state.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <h1>Single Pedestrians</h1>
 *
 * The single spawn algorithm divides the source in a grid based on the width of the pedestrians.
 * This grid is used to place newly spawn pedestrians. These points are called spawnPoints and
 * are saved as an 1D-array. Based on the Source Attribute values one of the four functions will
 * be used to select the next spawnPoints.
 *
 * <h2>{@link #getNextSpawnPoints(int, List)}</h2>
 * use the next free spawn point in order (0..n) to place the next pedestrian. This function will
 * try to place up to maxPoints pedestrian an will wrap around to spawnPoint 0 if needed. Also this
 * function will allow overlapping pedestrians a complete overlap is not allowed due to numerical
 * problems in OE-solvers.
 *
 * <h2>{@link #getNextRandomSpawnPoints(int, Random, List)}</h2>
 * same as above but the spawn points will be shuffled prior to iteration.
 *
 * <h2>{@link #getNextFreeSpawnPoints(int, List)}</h2>
 * same as above but it will be ensured that the new pedestrian does not overlap with any part of
 * an existing pedestrian.
 *
 * <h2>{@link #getNextFreeRandomSpawnPoints(int, Random, List)}</h2>
 * sam es above but the spawn points will be shuffled prior to iteration and overlapping will be
 * tested.
 *
 *
 * <h1>Groups</h1>
 *
 * Groups are spawn as a rectangle (axb) with the smallest possible deviation of a and b.  The
 * groupNumber is zero-based index counting possible spawn point for a group (see below). The
 * spawn positions (groupNumbers) will overlap. Depending on the selected Attributes the algorithm
 * will test in advance if the groupNumber is free (not occupied).
 *
 *   (0)    (1)    (2)    (3)    (4)    (5)    (6)    (7)    (8)   <-- groupNumber--------------
 * | **00 | 0**0 | 00** | 0000 | 0000 | 0000 | 0000 | 0000 | 0000 |-----------------------------
 * | **00 | 0**0 | 00** | **00 | 0**0 | 00** | 0000 | 0000 | 0000 |-----------------------------
 * | 0000 | 0000 | 0000 | **00 | 0**0 | 00** | **00 | 0**0 | 00** |-----------------------------
 * | 0000 | 0000 | 0000 | 0000 | 0000 | 0000 | **00 | 0**0 | 00** |-----------------------------
 *
 * <h2>{@link #getNextGroup(int, List)}  (without Random Object)</h2>
 * Iterate through the groupNumbers in order (0-->8) but remember the last used groupNumber for
 * each groupSize in the HashMap nextGroupPos. The Iteration order is generated with lambada
 * expressions. Also getNextGroup allows overlapping spawning a complete overlap is not allowed due
 * to numerical problems in OE-Solvers which would loop forever.
 *
 * <h2>{@link #getNextGroup(int, Random, List)} (with Random Object)</h2>
 * Same as before with the distinction that the groupNumbers as iterated in a random order. This
 * function will update the the HashMap nextGroupPos but this is not a problem because in this case
 * the nextGroupPos is not used.
 *
 * <h2>{@link #getNextFreeGroup(int, List)} (without Random Object)</h2>
 * This function will always iterate in order order (0-->8) and will use the first free groupNumber
 * available. This function ignores  the nextGroupPos HashMap.
 *
 * <h2>{@link #getNextFreeGroup(int, Random, List)} (with Random Object)</h2>
 * same as above but it will use a Randomize groupNumber Iterator.
 *
 */
public class SpawnArray {

	private static Logger logger = LogManager.getLogger(SpawnArray.class);
	public static final double OVERLAPP_EPSILON = 0.4;
	private final VPoint[] spawnPoints;
	private final VRectangle spawnElementBound;
	// number of spawn elements in x and y Dimension.
	private int xDim;
	private int yDim;
	private int nextIndex;
	// not an index put a way to calculate the index 1..n where n is the number of possible ways to place a given group
	// key: groupSize
	// value: number of next group.
	private HashMap<Integer, Integer> nextGroupPos;
	private HashMap<Integer, GroupPlacementHelper> groupPlacementHelpers;

	public SpawnArray(VRectangle bound, VRectangle spawnElementBound) {
		xDim = (int) (bound.width / spawnElementBound.width);
		yDim = (int) (bound.height / spawnElementBound.height);
		this.spawnElementBound = spawnElementBound;
//		System.out.printf("SpawnElement: %f | %f %n", spawnElementBound.width, spawnElementBound.height);

		double eX, eY;
		if (xDim * yDim <= 0) {
			xDim = (xDim == 0) ? 1 : xDim;
			yDim = (yDim == 0) ? 1 : yDim;

			spawnPoints = new VPoint[xDim * yDim];
			//offset left upper corner to center point.
			eX = (xDim == 1) ? bound.getCenterX() : spawnElementBound.x + spawnElementBound.width / 2;
			eY = (yDim == 1) ? bound.getCenterY() : spawnElementBound.y + spawnElementBound.height / 2;
			logger.info(String.format(
					"Dimension of Source is to small for at least one dimension to contain designated spawnElement with Bound (%.2f x %.2f) Set to (%d x %d)",
					spawnElementBound.width, spawnElementBound.height, xDim, yDim));

		} else {
			spawnPoints = new VPoint[xDim * yDim];
			//offset left upper corner to center point.
			eX = spawnElementBound.x + spawnElementBound.width / 2;
			eY = spawnElementBound.y + spawnElementBound.height / 2;
		}

		VPoint firstSpawnPoint = new VPoint(bound.x + eX, bound.y + eY);

		for (int i = 0; i < spawnPoints.length; i++) {
			spawnPoints[i] = firstSpawnPoint.add(new VPoint(2 * eX * (i % xDim), 2 * eY * (i / xDim)));
		}
		nextIndex = 0;
		nextGroupPos = new HashMap<>();
		groupPlacementHelpers = new HashMap<>();

	}

	/**
	 * @return maximum dimension of element. If VCircle or square it is the same in x and y. For
	 * other shapes the bounding box is used and from this the biggest dimension.
	 */
	public double getMaxElementDim() {
		return spawnElementBound.width > spawnElementBound.height ? spawnElementBound.width : spawnElementBound.height;
	}

	/**
	 * @return next SpawnPointIndex used to spawn underling spawnElementBound
	 */
	public int getNextSpawnPointIndex() {
		return nextIndex;
	}

	/**
	 * @return copy of spawnPoint used for underling source shape.
	 */
	public VPoint[] getSpawnPoints() {
		return Arrays.copyOf(spawnPoints, spawnPoints.length);
	}

	// spawn without checking for free space does not make sense.
	// Check here for a complete overlap (With epsilon) and use different cell in this case
	// caller must handle return null.
	@Deprecated
	public LinkedList<VPoint> getNextSpawnPoints(int maxPoints, final List<DynamicElement> neighbours) {
		return spawnPoints(maxPoints,
				neighbours,
				(n, p) -> n.getShape().getCentroid().equals(p, OVERLAPP_EPSILON) ,
				len -> startWith(nextIndex, len));	// define spawn order (use next index)
	}

	// spawn without checking for free space does not make sense.
	// Check here for a complete overlap (With epsilon) and use different cell in this case
	// caller must handle return null.
	@Deprecated
	public LinkedList<VPoint> getNextRandomSpawnPoints(int maxPoints, Random rnd, final List<DynamicElement> neighbours) {
		return spawnPoints(maxPoints,
				neighbours,
				(n, p) -> n.getShape().getCentroid().equals(p, OVERLAPP_EPSILON) ,
				len -> shufflePoints(rnd, len));	// define spawn order (use random index)
	}

	/**
	 * @param neighbours Test against this List. Caller must ensure that this neighbours are in the
	 *                   vicinity of the source bound.
	 * @return This function returns as many free points as possible. Caller must check how many
	 * points where found.
	 */
	public LinkedList<VPoint> getNextFreeSpawnPoints(int maxPoints, final List<DynamicElement> neighbours) {
		double d = getMaxElementDim() / 2; // radius.
		return  spawnPoints(maxPoints,
				neighbours,
				(n, p ) -> ((n.getShape().distance(p) < d) || n.getShape().contains(p)),
				len -> startWith(nextIndex, len)); // define spawn order (use next index)
	}

	public LinkedList<VPoint> getNextFreeRandomSpawnPoints(int maxPoints, Random rnd, final List<DynamicElement> neighbours) {
		double d = getMaxElementDim() / 2; // radius.

		return  spawnPoints(maxPoints,
					neighbours,
					(n, p ) -> ((n.getShape().distance(p) < d) || n.getShape().contains(p)),
					(len) -> shufflePoints(rnd, len)); // define spawn order (use random index)
	}

	/**
	 *
	 * @param maxPoints			Maximum number of Pedestrain to spawn
	 * @param neighbours		Pedestrian already spawned
	 * @param testOverlap		function defining if at the current position a neighbour already occupies the spot
	 * @param spawnPointOrder	function defining the order in which the spawn points are iterated.
	 * @return					new spawn points
	 */
	private LinkedList<VPoint> spawnPoints(int maxPoints, final List<DynamicElement> neighbours,
										   BiFunction<DynamicElement, VPoint, Boolean> testOverlap,
										   Function<Integer, ArrayList<Integer>> spawnPointOrder){
		LinkedList<VPoint> points = new LinkedList<>();

		// generate iterator order based on spawnPointOrder function.
		ArrayList<Integer> pointOrder = spawnPointOrder.apply(spawnPoints.length);

		ListIterator<Integer> iter = pointOrder.listIterator();
		while (iter.hasNext()){
			Integer next = iter.next();
			VPoint tmp = spawnPoints[next];
			boolean isOccupied = neighbours.parallelStream().anyMatch(n -> testOverlap.apply(n, tmp));

			if (!isOccupied){
				points.add(tmp.clone());
				if (points.size() == maxPoints) {
					// remember next spawn position. If this is the last on, wrap around and use
					// the first element of pointOrder ArrayList.
					nextIndex = iter.hasNext() ? iter.next() : pointOrder.get(0);
					break;
				}
			}
		}
		return points;
	}


	private ArrayList<Integer> shufflePoints(Random rnd, int len){
		ArrayList<Integer> ret = IntStream.range(0, len)
				.boxed().collect(Collectors.toCollection(ArrayList::new));
		Collections.shuffle(ret, rnd);
		ret.trimToSize();
		return ret;
	}

	private ArrayList<Integer> defaultOrder (int len){
		ArrayList<Integer> ret = IntStream.range(0, len).boxed().collect(Collectors.toCollection(ArrayList::new));
		ret.trimToSize();
		return  ret;
	}

	// ring buffer. start with
	private ArrayList<Integer> startWith (int start, int len){
		ArrayList<Integer> ret = IntStream.concat(IntStream.range(start, len), IntStream.range(0, start)).boxed()
				.collect(Collectors.toCollection(ArrayList::new));
		ret.trimToSize();
		return ret;
	}

	// Groups

	@Deprecated
	public LinkedList<VPoint> getNextGroup(int groupSize, final List<DynamicElement> neighbours) {
		GroupPlacementHelper pHelper = getHelper(groupSize);
		return nextFreeGroupPos(pHelper,
				neighbours,
				(n,p) -> n.getShape().getCentroid().equals(p, OVERLAPP_EPSILON),
				len -> startWith(nextGroupPos.getOrDefault(pHelper.getGroupSize(), 0), len));
	}

	public LinkedList<VPoint> getNextGroup(int groupSize, Random rnd, final List<DynamicElement> neighbours) {
		GroupPlacementHelper pHelper = getHelper(groupSize);
		return nextFreeGroupPos(pHelper,
				neighbours,
				(n,p) -> n.getShape().getCentroid().equals(p, OVERLAPP_EPSILON),
				len -> shufflePoints(rnd, len));
	}

	public LinkedList<VPoint> getNextFreeGroup(int groupSize, final List<DynamicElement> neighbours) {
		GroupPlacementHelper pHelper = getHelper(groupSize);
		double d = getMaxElementDim() / 2; // radius.
		return nextFreeGroupPos(pHelper,
			neighbours,
			(n, p) -> ((n.getShape().distance(p) < d) || n.getShape().contains(p)),
			this::defaultOrder);
	}

	public LinkedList<VPoint> getNextFreeGroup(int groupSize, Random rnd, final List<DynamicElement> neighbours) {
		GroupPlacementHelper pHelper = getHelper(groupSize);
		double d = getMaxElementDim() / 2; // radius.
		return nextFreeGroupPos(pHelper,
								neighbours,
								(n, p) -> ((n.getShape().distance(p) < d) || n.getShape().contains(p)),
								len -> shufflePoints(rnd, len));
	}

	/**
	 *
	 * @param pHelper			Helper object to address spawnPoints based on groupNumber and
	 *                          interGroupIndex see class comment for definition of groupNumber and
	 *                          interGroupIndex
	 * @param neighbours		List of dynamic element to test for overlap
	 * @param testOverlap		Function to test if a neighbour occupies the current groupNumber
	 * @param spawnPointOrder	ArrayList generator to define iteration order of groupNumbers
	 * @return					List of spawnPoints used for the next group.
	 */
	private LinkedList<VPoint> nextFreeGroupPos(
			GroupPlacementHelper pHelper, final List<DynamicElement> neighbours,
			BiFunction<DynamicElement, VPoint, Boolean> testOverlap,
			Function<Integer, ArrayList<Integer>> spawnPointOrder) {

		int groupSize = pHelper.getGroupSize();
		if (groupSize > spawnPoints.length)
			throw new IndexOutOfBoundsException("GroupSize: " + groupSize
					+ "to big for source. Max Groupsize of source is " + spawnPoints.length);

		LinkedList<VPoint> points = new LinkedList<>();

		// generate iterator order based on spawnPointOrder function.
		ArrayList<Integer> groupNumbers = spawnPointOrder.apply(pHelper.getOverlappingGroupCount());

		ListIterator<Integer> iter = groupNumbers.listIterator();
		while (iter.hasNext()) {
			Integer next = iter.next();
			for (int i = 0; i < groupSize; i++) {
				int index = pHelper.getOverlappingIndex(next, i);
				VPoint p = spawnPoints[index].clone();
				boolean isOccupied = neighbours.parallelStream().anyMatch(n -> testOverlap.apply(n, p));
				if (!isOccupied) {
					points.add(p);
				} else {
					points = new LinkedList<>();
					break;
				}
			}

			if (points.size() == groupSize) {
				// remember next position for groupSize for next spawn event. this is the last position
				// wrap around and use first element of groupNumbers ArrayList
				nextGroupPos.put(groupSize, iter.hasNext() ? iter.next() : groupNumbers.get(0));
				break;
			}

		}
		return points;
	}

	private GroupPlacementHelper getHelper(int groupSize) {
		GroupPlacementHelper pHelper;
		if (groupPlacementHelpers.containsKey(groupSize)) {
			pHelper = groupPlacementHelpers.get(groupSize);
		} else {
			pHelper = new GroupPlacementHelper(xDim, yDim, groupSize);
			groupPlacementHelpers.put(groupSize, pHelper);
		}
		return pHelper;
	}
}

