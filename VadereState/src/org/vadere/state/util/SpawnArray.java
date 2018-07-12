package org.vadere.state.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Defines spawn points for a source and returns valid coordinates for spawning.
 */
public class SpawnArray {

	private static Logger logger = LogManager.getLogger(SpawnArray.class);
	private final VPoint[] spawnPoints;
	private final VRectangle spawnElementBound;
	// number of spawn elements in x and y Dimension.
	private int xDim;
	private int yDim;
	private int nextPoint;
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
		nextPoint = 0;
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
		return nextPoint;
	}

	/**
	 * @return copy of spawnPoint used for underling source shape.
	 */
	public VPoint[] getSpawnPoints() {
		return Arrays.copyOf(spawnPoints, spawnPoints.length);
	}

	public VPoint getNextSpawnPoint() {
		VPoint ret = spawnPoints[nextPoint].clone();
		nextPoint = (nextPoint + 1) % spawnPoints.length;
		return ret;
	}

	public VPoint getNextRandomSpawnPoint(Random rnd) {
		int index = rnd.nextInt(spawnPoints.length);
		return spawnPoints[index];
	}

	/**
	 * @param neighbours Test against this List. Caller must ensure that this neighbours are in the
	 *                   vicinity of the source bound.
	 * @return first free space within the spawn points.
	 */
	public VPoint getNextFreeSpawnPoint(final List<DynamicElement> neighbours) {
		double d = getMaxElementDim() / 2; // radius.
		for (VPoint p : spawnPoints) {
			boolean overlap = neighbours.parallelStream().anyMatch(n -> ((n.getShape().distance(p) < d) || n.getShape().contains(p)));
			if (!overlap) {
				return p.clone();
			}
		}
		return null;
	}

	/**
	 * @param neighbours Test against this List. Caller must ensure that this neighbours are in the
	 *                   vicinity of the source bound.
	 * @return This function returns as many free points as possible. Caller must check how many
	 * points where found.
	 */
	public LinkedList<VPoint> getNextFreeSpawnPoints(int maxPoints, final List<DynamicElement> neighbours) {
		double d = getMaxElementDim() / 2; // radius.
		LinkedList<VPoint> points = new LinkedList<>();
		for (VPoint p : spawnPoints) {
			boolean overlap = neighbours.parallelStream().anyMatch(n -> ((n.getShape().distance(p) < d) || n.getShape().contains(p)));
			if (!overlap) {
				points.add(p);
				if (points.size() == maxPoints) {
					break;
				}
			}
		}
		return points;
	}

	public LinkedList<VPoint> getNextFreeRandomSpawnPoints(int maxPoints, Random rnd, final List<DynamicElement> neighbours) {
		double d = getMaxElementDim() / 2; // radius.
		LinkedList<VPoint> points = new LinkedList<>();
		List<Integer> randInt = IntStream.range(0, spawnPoints.length).boxed().collect(Collectors.toList());
		Collections.shuffle(randInt, rnd);
		for (Integer i : randInt) {
			VPoint p = spawnPoints[i];
			boolean overlap = neighbours.parallelStream().anyMatch(n -> ((n.getShape().distance(p) < d) || n.getShape().contains(p)));
			if (!overlap) {
				points.add(p);
				if (points.size() == maxPoints) {
					break;
				}
			}
		}
		return points;
	}

	// Groups

	/**
	 * This function only spawns non overlapping groups. This means that for instance within a
	 * source of the size 4x4 there are only 4 possible locations to spawn a group of the size 4
	 *
	 * **00 | 00** | 0000 | 0000 |------------------------------------------------------------------
	 * **00 | 00** | 0000 | 0000 |------------------------------------------------------------------
	 * 0000 | 0000 | **00 | 00** |------------------------------------------------------------------
	 * 0000 | 0000 | **00 | 00** |------------------------------------------------------------------
	 *
	 * This function does not test if the space is occupied.
	 *
	 * @param groupSize size of group which should be spawned
	 * @return Point List
	 */
	public LinkedList<VPoint> getNextGroup(int groupSize) {
		return nextGroup(groupSize, null);
	}

	public LinkedList<VPoint> getNextGroup(int groupSize, Random rnd) {
		return nextGroup(groupSize, rnd);
	}

	private LinkedList<VPoint> nextGroup(int groupSize, Random rnd) {
		if (groupSize > spawnPoints.length)
			throw new IndexOutOfBoundsException("GroupSize: " + groupSize
					+ "to big for source. Max GroupSize of source is " + spawnPoints.length);

		GroupPlacementHelper pHelper;
		if (groupPlacementHelpers.containsKey(groupSize)) {
			pHelper = groupPlacementHelpers.get(groupSize);
		} else {
			pHelper = new GroupPlacementHelper(xDim, yDim, groupSize);
			groupPlacementHelpers.put(groupSize, pHelper);
		}

		int groupNumber;
		if (rnd != null) {
			groupNumber = rnd.nextInt(pHelper.getNoneOverlappingGroupCount());
		} else {
			groupNumber = nextGroupPos.getOrDefault(groupSize, 0);
		}

		LinkedList<VPoint> points = new LinkedList<>();
		for (int i = 0; i < groupSize; i++) {
			// move index in group
			int index = pHelper.getNoneOverlappingIndex(groupNumber, i);
			points.add(spawnPoints[index].clone());
		}
		nextGroupPos.put(groupSize, pHelper.nextNoneOverlappingGroupNumber(groupNumber));

		return points;
	}

	/**
	 * This function only can spawn overlapping groups but the possible location is tested if it is
	 * free. A source of the size 4x4 hast 9 possible spawn locations for a group of the size 4.
	 * Each location is test if all spots within the group are free. If so this location is return.
	 *
	 * | **00 | 0**0 | 00** | 0000 | 0000 | 0000 | 0000 | 0000 | 0000 |-----------------------------
	 * | **00 | 0**0 | 00** | **00 | 0**0 | 00** | 0000 | 0000 | 0000 |-----------------------------
	 * | 0000 | 0000 | 0000 | **00 | 0**0 | 00** | **00 | 0**0 | 00** |-----------------------------
	 * | 0000 | 0000 | 0000 | 0000 | 0000 | 0000 | **00 | 0**0 | 00** |-----------------------------
	 *
	 * @param groupSize size of group which should be spawned
	 * @return Point List or null if no free location is found for given group size.
	 */
	public LinkedList<VPoint> getNextFreeGroup(int groupSize, final List<DynamicElement> neighbours) {
		return nextFreeGroupPos(groupSize, null, neighbours);
	}

	public LinkedList<VPoint> getNextFreeGroup(int groupSize, Random rnd, final List<DynamicElement> neighbours) {
		return nextFreeGroupPos(groupSize, rnd, neighbours);
	}

	private LinkedList<VPoint> nextFreeGroupPos(int groupSize, Random rnd, final List<DynamicElement> neighbours) {
		if (groupSize > spawnPoints.length)
			throw new IndexOutOfBoundsException("GroupSize: " + groupSize
					+ "to big for source. Max Groupsize of source is " + spawnPoints.length);

		GroupPlacementHelper pHelper;
		if (groupPlacementHelpers.containsKey(groupSize)) {
			pHelper = groupPlacementHelpers.get(groupSize);
		} else {
			pHelper = new GroupPlacementHelper(xDim, yDim, groupSize);
			groupPlacementHelpers.put(groupSize, pHelper);
		}

		double d = getMaxElementDim() / 2; // radius.
		LinkedList<VPoint> points = new LinkedList<>();
		//over all overlapping groups in source
		boolean isOccupied = true;

		List<Integer> groupNumbers;
		if (rnd != null) {
			groupNumbers = IntStream.range(0, pHelper.getOverlappingGroupCount())
					.boxed().collect(Collectors.toList());
			Collections.shuffle(groupNumbers, rnd);
		} else {
			groupNumbers = IntStream.range(0, pHelper.getOverlappingGroupCount())
					.boxed().collect(Collectors.toList());
		}

		for (Integer groupNumber : groupNumbers) {
			for (int i = 0; i < groupSize; i++) {
				int index = pHelper.getOverlappingIndex(groupNumber, i);
				VPoint p = spawnPoints[index].clone();
				isOccupied = neighbours.parallelStream().anyMatch(n -> ((n.getShape().distance(p) < d) || n.getShape().contains(p)));
				if (!isOccupied) {
					points.add(p);
				} else {
					points = new LinkedList<>();
					break;
				}
			}
			// if a group was found with no overlapping point break and return group.
			if (!isOccupied)
				break;
		}

		if (points.size() < groupSize) {
			return null;
		} else {
			return points;
		}
	}

}

