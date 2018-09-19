package org.vadere.state.util;

/**
 * Simplifies placement of a group within a {@link SpawnArray}. There are two placement strategies
 * supported.
 * <li>NoneOverlapping:</li>
 * With this strategy a group spawns do not overlap. E.g The first 2x2 Group will start at (0,0) and
 * the second at (2,0) under the assumption the underlying source is big enough to hold two 2x2
 * groups in the x dimension.
 *
 * <li>Overlapping:</li>
 * With this strategy the second group will start at (1,0)
 */
public class GroupPlacementHelper {

	private final int xBound;
	private final int yBound;

	private final int groupSize;
	private final int groupDimX;
	private final int groupDimY;

	private final int noneOverlapXGroupCount;
	private final int noneOverlapYGroupCount;

	private final int overlapXGroupCount;
	private final int overlapYGroupCount;


	public GroupPlacementHelper(int xBound, int yBound, int groupSize) {
		if (groupSize > xBound * yBound)
			throw new IndexOutOfBoundsException("GroupSize: " + groupSize
					+ "to big for given Bound " + xBound + " x " + yBound);

		this.xBound = xBound;
		this.yBound = yBound;
		this.groupSize = groupSize;

		int dimGx, dimGy;


		dimGx = (int) Math.ceil(Math.sqrt(groupSize));
		dimGx = (dimGx > xBound) ? xBound : dimGx;
		// dimGy set to minimize lost space in resulting rectangle (or square if groupSize is a square number)
		dimGy = dimGx * (dimGx - 1) < groupSize ? dimGx : dimGx - 1;
		this.groupDimX = dimGx;
		this.groupDimY = dimGy;

		this.noneOverlapXGroupCount = xBound / dimGx;
		this.noneOverlapYGroupCount = yBound / dimGy;

		this.overlapXGroupCount = xBound - (dimGx - 1);
		this.overlapYGroupCount = yBound - (dimGy - 1);
	}

	/**
	 * @param groupNumber zero-Based number of group of groupSize with the noneOverlapping strategy
	 * @return zero-Based index within {@link SpawnArray} corresponding to start index of
	 * groupNumber.
	 */
	public int getNoneOverlappingStart(int groupNumber) {
		return (groupNumber % noneOverlapXGroupCount) * groupDimX +            // offset in x
				(groupNumber / noneOverlapXGroupCount) * xBound * groupDimY; // offset in y
	}

	/**
	 * NoneOverlapping strategy
	 *
	 * @param groupNumber zero-Based number of group
	 * @param i           zero-Based index within group. Must be smaller than groupSize
	 * @return zero-Based index within {@link SpawnArray} corresponding to groupNumber and index
	 * i
	 */
	public int getNoneOverlappingIndex(int groupNumber, int i) {
		assert i < groupSize;
		int start = getNoneOverlappingStart(groupNumber);
		return start + (i % groupDimX) + (i / groupDimX) * xBound;
	}

	public int nextNoneOverlappingGroupNumber(int oldGroupNumber) {
		return (oldGroupNumber + 1) % (noneOverlapXGroupCount * noneOverlapYGroupCount);
	}

	/**
	 * @return Number of groups based on noneOverlapping strategy
	 */
	public int getNoneOverlappingGroupCount() {
		return noneOverlapXGroupCount * noneOverlapYGroupCount;
	}


	/**
	 * @param groupNumber zero-Based number of group of groupSize with the overlapping strategy
	 * @return zero-Based index within {@link SpawnArray} corresponding to groupNumber and index i
	 */
	public int getOverlappingStart(int groupNumber) {
		return (groupNumber % overlapXGroupCount) +            // offset in x
				(groupNumber / overlapXGroupCount) * xBound;    // offset in y (groupDimY not needed)
	}

	/**
	 * Overlapping strategy
	 *
	 * @param groupNumber zero-Based number of group
	 * @param i           zero-Based index within group. Must be smaller than groupSize
	 * @return zero-Based index within {@link SpawnArray} corresponding to groupNumber and index
	 * i
	 */
	public int getOverlappingIndex(int groupNumber, int i) {
		assert i < groupSize;
		int start = getOverlappingStart(groupNumber);
		return start + (i % groupDimX) + (i / groupDimX) * xBound;
	}

	public int getOverlappingGroupCount() {
		return overlapXGroupCount * overlapYGroupCount;
	}

	public int getGroupSize() {
		return groupSize;
	}
}
