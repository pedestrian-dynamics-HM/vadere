package org.vadere.simulator.control.util;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.*;
import java.util.function.Function;

/**
 * <h1>Groups</h1>
 *
 * Groups are spawn as a rectangle (axb) with the smallest possible deviation of a and b.  The
 * groupNumber is zero-based index counting possible spawn point for a group (see below). The spawn
 * positions (groupNumbers) will overlap. Depending on the selected Attributes the algorithm will
 * test in advance if the groupNumber is free (not occupied).
 *
 * (0)    (1)    (2)    (3)    (4)    (5)    (6)    (7)    (8)   <-- groupNumber-------------- |
 * **00 | 0**0 | 00** | 0000 | 0000 | 0000 | 0000 | 0000 | 0000 |----------------------------- |
 * **00 | 0**0 | 00** | **00 | 0**0 | 00** | 0000 | 0000 | 0000 |----------------------------- |
 * 0000 | 0000 | 0000 | **00 | 0**0 | 00** | **00 | 0**0 | 00** |----------------------------- |
 * 0000 | 0000 | 0000 | 0000 | 0000 | 0000 | **00 | 0**0 | 00** |-----------------------------
 *
 * <h2>{@link #getNextGroup(int, List)}  (without Random Object)</h2>
 * Iterate through the groupNumbers in order (0-->8) but remember the last used groupNumber for each
 * groupSize in the HashMap nextGroupPos. The Iteration order is generated with lambada expressions.
 * Also getNextGroup allows overlapping spawning a complete overlap is not allowed due to numerical
 * problems in OE-Solvers which would loop forever.
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
 */
public class GroupSpawnArray extends SpawnArray {

	// not an index put a way to calculate the index 1..n where n is the number of possible ways to place a given group
	// key: groupSize
	// value: number of next group.
	private final HashMap<Integer, Integer> nextGroupPos;
	private final HashMap<Integer, GroupPlacementHelper> groupPlacementHelpers;


	public GroupSpawnArray(final VShape boundShape,
						   final VRectangle spawnElementBound,
						   Function<VPoint, VShape> shapeProducer,
						   SpawnOverlapCheck testFreeSpace,
						   AttributesSpawner spawnerAttributes) {
		super(boundShape, spawnElementBound, shapeProducer, testFreeSpace, spawnerAttributes);

		nextGroupPos = new HashMap<>();
		groupPlacementHelpers = new HashMap<>();
	}


	private ArrayList<Integer> shufflePoints(ArrayList<Integer> list, Random rnd) {
		Collections.shuffle(list, rnd);
		list.trimToSize();
		return list;
	}

	// ring buffer. start with
	private ArrayList<Integer> startWith(ArrayList<Integer> list, int start) {
		Integer startIndex = list.indexOf(start);
		startIndex = startIndex == -1 ? 0 : startIndex;
		List<Integer> list1 = list.subList(startIndex, list.size());
		List<Integer> list2 = list.subList(0, startIndex);
		ArrayList<Integer> ret = new ArrayList<>(list.size());
		ret.addAll(list1);
		ret.addAll(list2);
		return ret;
	}

	// Groups
	public List<VPoint> getDefaultGroup(int groupSize) {
		GroupPlacementHelper pHelper = getHelper(groupSize);
		int firstValidIndex = pHelper.getValidSpawnPointsForGroupInBound().get(0);
		List<VPoint> points = new ArrayList<>();
		for (int i = 0; i < groupSize; i++) {
			int index = validSpawnPointMapInBoundShape.get(pHelper.getOverlappingIndex(firstValidIndex, i));
			VPoint candidatePoint = allowedSpawnPoints.get(index).clone();
			points.add(candidatePoint);
		}
		return points;
	}

	@Deprecated
	public LinkedList<VPoint> getNextGroup(int groupSize, @NotNull final List<VShape> blockPedestrianShapes) {
		GroupPlacementHelper pHelper = getHelper(groupSize);
		return nextFreeGroupPos(pHelper,
				blockPedestrianShapes,
				startWith(pHelper.getValidSpawnPointsForGroupInBound(), nextGroupPos.getOrDefault(pHelper.getGroupSize(), 0))
		);
	}

	public LinkedList<VPoint> getNextGroup(int groupSize, @NotNull final Random rnd, @NotNull final List<VShape> blockPedestrianShapes) {
		GroupPlacementHelper pHelper = getHelper(groupSize);
		return nextFreeGroupPos(pHelper,
				blockPedestrianShapes,
				shufflePoints(pHelper.getValidSpawnPointsForGroupInBound(), rnd));
	}

	public LinkedList<VPoint> getNextFreeGroup(int groupSize, @NotNull final List<VShape> blockPedestrianShapes) {
		GroupPlacementHelper pHelper = getHelper(groupSize);
		return nextFreeGroupPos(pHelper, blockPedestrianShapes, pHelper.getValidSpawnPointsForGroupInBound());
	}

	public LinkedList<VPoint> getNextFreeGroup(int groupSize, Random rnd, @NotNull final List<VShape> blockPedestrianShapes) {
		GroupPlacementHelper pHelper = getHelper(groupSize);
		return nextFreeGroupPos(pHelper,
				blockPedestrianShapes,
				shufflePoints(pHelper.getValidSpawnPointsForGroupInBound(), rnd));
	}

	/**
	 * @param pHelper               Helper object to address allowedSpawnPoints based on groupNumber
	 *                              and interGroupIndex see class comment for definition of
	 *                              groupNumber and interGroupIndex
	 * @param blockPedestrianShapes List of Shapes element to test for overlap
	 * @param groupNumbers          ArrayList iteration order of groupNumbers
	 * @return List of allowedSpawnPoints used for the next group.
	 */
	private LinkedList<VPoint> nextFreeGroupPos(
			GroupPlacementHelper pHelper, @NotNull final List<VShape> blockPedestrianShapes,
			ArrayList<Integer> groupNumbers) {

		int groupSize = pHelper.getGroupSize();
		if (groupSize > allowedSpawnPoints.size())
			throw new IndexOutOfBoundsException("GroupSize: " + groupSize
					+ "to big for source. Max Groupsize of source is " + allowedSpawnPoints.size());

		LinkedList<VPoint> points = new LinkedList<>();

		ListIterator<Integer> iter = groupNumbers.listIterator();
		while (iter.hasNext()) {
			Integer next = iter.next();
			for (int i = 0; i < groupSize; i++) {
				int index = validSpawnPointMapInBoundShape.get(pHelper.getOverlappingIndex(next, i));
				VPoint candidatePoint = allowedSpawnPoints.get(index).clone();
				VShape candidateShape = shapeProducer.apply(candidatePoint);
				boolean isFreeSpace = testFreeSpace.checkFreeSpace(candidateShape, blockPedestrianShapes);
				if (isFreeSpace) {
					points.add(candidatePoint);
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
			pHelper = new GroupPlacementHelper(xDim, yDim, groupSize, validSpawnPointMapInBoundShape);
			groupPlacementHelpers.put(groupSize, pHelper);
		}
		return pHelper;
	}
}

