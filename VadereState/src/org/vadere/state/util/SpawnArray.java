package org.vadere.state.util;

import org.lwjgl.system.CallbackI;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import sun.java2d.pipe.ValidatePipe;

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

	// number of spawn elements in x and y Dimension.
	private final int xDim;
	private final int yDim;
	private final VPoint[] spawnPoints;
	private final VRectangle spawnElementBound;
	private int nextPoint;
	// not an index put a way to calculate the index 1..n where n is the number of possible ways to place a given group
	// key: groupSize
	// value: number of next group.
	private HashMap<Integer, Integer> nextGroupPos;


	public SpawnArray(VRectangle bound, VRectangle spawnElementBound) {
		xDim = (int) (bound.width / spawnElementBound.width);
		yDim = (int) (bound.height / spawnElementBound.height);
		this.spawnElementBound = spawnElementBound;
		spawnPoints = new VPoint[xDim * yDim];

		//offset left upper corner to center point.
		double eX = spawnElementBound.x + spawnElementBound.width / 2;
		double eY = spawnElementBound.y + spawnElementBound.height / 2;
		VPoint firstSpawnPoint = new VPoint(bound.x + eX, bound.y + eY);

		for (int i = 0; i < spawnPoints.length; i++) {
			spawnPoints[i] = firstSpawnPoint.add(new VPoint(2 * eX * (i % xDim), 2 * eY * (i / xDim)));
		}
		nextPoint = 0;
		nextGroupPos = new HashMap<>();

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
	public VPoint getNextFreePoint(final List<DynamicElement> neighbours) {
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
	public LinkedList<VPoint> getNextFreePoints(int maxPoints, final List<DynamicElement> neighbours) {
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

	public LinkedList<VPoint> getNextFreeRandomPoints(int maxPoints, Random rnd, final List<DynamicElement> neighbours) {
		double d = getMaxElementDim() / 2; // radius.
		LinkedList<VPoint> points = new LinkedList<>();
		List<Integer> randInt = IntStream.range(0, spawnPoints.length).boxed().collect(Collectors.toList());
		Collections.shuffle(randInt);
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

	/**
	 * This function only spawns non overlapping groups. This means that for instance within a
	 * source of the size 4x4 there are only 4 possible locations to spawn a group of the size 4
	 *
	 * **00 | 00** | 0000 | 0000 **00 | 00** | 0000 | 0000 0000 | 0000 | **00 | 00** 0000 | 0000 |
	 * **00 | 00**
	 *
	 * This function does not test if the space is occupied.
	 *
	 * @param groupSize size of group which should be spawned
	 * @return Point List
	 */
	public LinkedList<VPoint> getNextGroup(int groupSize) {
		return nextGroup(groupSize, null);
	}

	/**
	 * Same as getNextGroup(int groupSize) but the selection of the possible spawn location is
	 * chosen randomly
	 */
	public LinkedList<VPoint> getNextGroup(int groupSize, Random rnd) {
		return nextGroup(groupSize, rnd);
	}

	private LinkedList<VPoint> nextGroup(int groupSize, Random rnd) {
		if (groupSize > spawnPoints.length)
			throw new IndexOutOfBoundsException("GroupSize: " + groupSize
					+ "to big for source. Max Groupsize of source is " + spawnPoints.length);

		// dimGx width of smallest square containing a Group of size groupSize
		int dimGx = (int) Math.ceil(Math.sqrt(groupSize));
		// dimGy set to minimize lost space in resulting rectangle (or square if groupSize is a square number)
		int dimGy = dimGx * dimGx == groupSize ? dimGx : dimGx - 1;


		int maxXGroups = xDim / dimGx; // only this many groups in x dim
		int maxYGroups = yDim / dimGy; // only this many groups ind y dim


		int groupNumber = nextGroupPos.getOrDefault(groupSize, 0);
		int start;
		if (rnd != null) {
			start = rnd.nextInt(maxXGroups * maxYGroups);
		} else {
			start = (groupNumber % maxXGroups) * dimGx + (groupNumber / maxXGroups) * xDim * dimGy;
		}


		LinkedList points = new LinkedList();
		for (int i = 0; i < groupSize; i++) {
			// move index in group
			int index = start + (i % dimGx) + (i / dimGx) * xDim;
			points.add(spawnPoints[index].clone());
		}
		nextGroupPos.put(groupSize, (groupNumber + 1) % (maxXGroups * maxYGroups));

		return points;
	}

	/**
	 * This function only can spawn overlapping groups but the possible location is tested if it is
	 * free. A source of the size 4x4 hast 9 possible spawn locations for a group of the size 4.
	 * Each location is test if all spots within the group are free. If so this location is return.
	 *
	 * **00 | 0**0 | 00** | 0000 | 0000 | 0000 | 0000 | 0000 | 0000 **00 | 0**0 | 00** | **00 | 0**0
	 * | 00** | 0000 | 0000 | 0000 0000 | 0000 | 0000 | **00 | 0**0 | 00** | **00 | 0**0 | 00** 0000
	 * | 0000 | 0000 | 0000 | 0000 | 0000 | **00 | 0**0 | 00**
	 *
	 * @param groupSize size of group which should be spawned
	 * @return Point List or null if no free location is found for given group size.
	 */
	public LinkedList<VPoint> getNextFreeGroupPos(int groupSize, final List<DynamicElement> neighbours) {
		if (groupSize > spawnPoints.length)
			throw new IndexOutOfBoundsException("GroupSize: " + groupSize
					+ "to big for source. Max Groupsize of source is " + spawnPoints.length);

		// dimGx width of smallest square containing a Group of size groupSize
		int dimGx = (int) Math.ceil(Math.sqrt(groupSize));
		// dimGy set to minimize lost space in resulting rectangle (or square if groupSize is a square number)
		int dimGy = dimGx * dimGx == groupSize ? dimGx : dimGx - 1;

		int maxXGroupsWithOverlap = xDim - (dimGx - 1);
		int maxYGroupsWithOverlap = yDim - (dimGy - 1);

		double d = getMaxElementDim() / 2; // radius.
		LinkedList<VPoint> points = new LinkedList<>();
		//over all overlapping groups in source
		boolean overlap = true;
		for (int groupNumber = 0; groupNumber < maxXGroupsWithOverlap * maxYGroupsWithOverlap; groupNumber++) {
			int start = (groupNumber % maxXGroupsWithOverlap) + (groupNumber / maxXGroupsWithOverlap) * xDim;
			for (int i = 0; i < groupSize; i++) {
				int index = start + (i % dimGx) + (i / dimGx) * xDim;
				VPoint p = spawnPoints[index].clone();
				overlap = neighbours.parallelStream().anyMatch(n -> ((n.getShape().distance(p) < d) || n.getShape().contains(p)));
				if (!overlap) {
					points.add(p);
				} else {
					points = new LinkedList<>();
					break;
				}
			}
			// if a group was found with no overlapping point break and return group.
			if (!overlap)
				break;
		}

		if (points.size() < groupSize) {
			return null;
		} else {
			return points;
		}
	}

}

