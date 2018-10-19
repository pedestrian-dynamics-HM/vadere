package org.vadere.simulator.control.util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simplifies placement of a group within a  There are two placement strategies supported.
 * <li>NoneOverlapping:</li>
 * With this strategy a group spawns do not overlap. E.g The first 2x2 Group will start at (0,0) and
 * the second at (2,0) under the assumption the underlying source is big enough to hold two 2x2
 * groups in the x dimension.
 *
 * <li>Overlapping:</li>
 * With this strategy the second group will start at (1,0)
 */
public class GroupPlacementHelper {

	private final int boundedShapeGridCellsX;
	private final int boundedShapeGridCellsY;

	private final int groupSize;
	private final int groupDimX;
	private final int groupDimY;

	private final int noneOverlapXGroupCount;
	private final int noneOverlapYGroupCount;

	private final int groupPlacementCountX;
	private final int groupPlacementCountY;

	private ArrayList<Integer> validSpawnPointsForGroupInBound;

	public GroupPlacementHelper(int boundedShapeGridCellsX, int boundedShapeGridCellsY,
								int groupSize, HashMap<Integer, Integer> validSpawnPointMapInBoundShape) {
		if (groupSize > boundedShapeGridCellsX * boundedShapeGridCellsY)
			throw new IndexOutOfBoundsException("GroupSize: " + groupSize
					+ "to big for given Bound " + boundedShapeGridCellsX + " x " + boundedShapeGridCellsY);

		this.boundedShapeGridCellsX = boundedShapeGridCellsX;
		this.boundedShapeGridCellsY = boundedShapeGridCellsY;
		this.groupSize = groupSize;

		int dimGx, dimGy;


		// dimension of smallest square contain a group of size groupSize
		dimGx = (int) Math.ceil(Math.sqrt(groupSize));
		dimGx = (dimGx > boundedShapeGridCellsX) ? boundedShapeGridCellsX : dimGx;
		// dimGy set to minimize lost space in resulting rectangle (or square if groupSize is a square number)
		dimGy = dimGx * (dimGx - 1) < groupSize ? dimGx : dimGx - 1;
		this.groupDimX = dimGx;
		this.groupDimY = dimGy;

		this.noneOverlapXGroupCount = boundedShapeGridCellsX / dimGx;
		this.noneOverlapYGroupCount = boundedShapeGridCellsY / dimGy;

		//
		this.groupPlacementCountX = boundedShapeGridCellsX - (dimGx - 1);
		this.groupPlacementCountY = boundedShapeGridCellsY - (dimGy - 1);

		validSpawnPointsForGroupInBound = new ArrayList<>();
		for (int i = 0; i < getOverlappingGroupCount(); i++) { // i  group spawn location
			if (isGridCellWithinSource(validSpawnPointMapInBoundShape, i)) {
				validSpawnPointsForGroupInBound.add(i);
			}
		}
	}

	/**
	 * @param validSpawnPointMapInBoundShape mapping of rectangular bound grid to valid coordinates
	 *                                       within the source shape
	 * @param groupIndex                     groupIndex specifying the first ped within one group.
	 * @return true if all positions within the group are contained within the source
	 * shape.
	 */
	boolean isGridCellWithinSource(HashMap<Integer, Integer> validSpawnPointMapInBoundShape, int groupIndex) {
		for (int pedIndexInGroup = 0; pedIndexInGroup < groupSize; pedIndexInGroup++) {
			boolean isValid = validSpawnPointMapInBoundShape.containsKey(getOverlappingIndex(groupIndex, pedIndexInGroup));
			if (!isValid) {
				return false;
			}
		}
		return true;
	}


	/**
	 * @param groupNumber zero-Based number of group of groupSize with the overlapping strategy
	 * @return zero-Based index within {@link GroupSpawnArray} corresponding to groupNumber and
	 * index i
	 */
	public int getOverlappingStart(int groupNumber) {
		return (groupNumber % groupPlacementCountX) +            // offset in x
				(groupNumber / groupPlacementCountX) * boundedShapeGridCellsX;    // offset in y (groupDimY not needed)
	}

	/**
	 * Overlapping strategy
	 *
	 * @param groupNumberInBound zero-Based number of group
	 * @param i                  zero-Based index within group. Must be smaller than groupSize
	 * @return zero-Based index within {@link GroupSpawnArray} corresponding to groupNumber and
	 * index i
	 */
	public int getOverlappingIndex(int groupNumberInBound, int i) {
		assert i < groupSize;
		int start = getOverlappingStart(groupNumberInBound);
		return start + (i % groupDimX) + (i / groupDimX) * boundedShapeGridCellsX;
	}

	public int getOverlappingGroupCount() {
		return groupPlacementCountX * groupPlacementCountY;
	}

	public ArrayList<Integer> getValidSpawnPointsForGroupInBound() {
		return validSpawnPointsForGroupInBound;
	}

	public int getGroupSize() {
		return groupSize;
	}
}
