package org.vadere.util.potential;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.FloorDiscretizer;
import org.vadere.util.potential.PathFindingTag;

public class TestFloorDiscretizer {
	private VRectangle shapeToDiscretize;

	private double gridWidth = 2;
	private double gridHeight = 2;
	private double gridResolution = 1;
	private CellGrid cellGrid;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cellGrid = new CellGrid(gridWidth, gridHeight, gridResolution, new CellState(0.0, PathFindingTag.Undefined));
	}

	/**
	 * Test method for
	 * {@link org.vadere.util.potential.FloorDiscretizer#setGridDistanceValuesForShape(CellGrid, org.vadere.util.geometry.shapes.VShape, PathFindingTag)}.
	 */
	@Test
	public void testSetGridDistanceValuesForShape_halfGrid() {
		shapeToDiscretize = new VRectangle(0, 0, gridWidth, gridHeight / 2);

		FloorDiscretizer.setGridDistanceValuesForShape(cellGrid, shapeToDiscretize, PathFindingTag.Target,
				PathFindingTag.Reachable);

		// test if all the values are set to zero even only half the grid was filled (i.e. the
		// initial values are zero)
		double counter = 0;
		for (int y = 0; y < cellGrid.numPointsY; y++) {
			for (int x = 0; x < cellGrid.numPointsX; x++) {
				counter += cellGrid.getValue(x, y).potential;
			}
		}
		assertEquals(0, counter, 1e-8);

		// test if all states inside the shape to discretize are set to target
		for (int y = 0; y < cellGrid.numPointsY / 2; y++) {
			for (int x = 0; x < cellGrid.numPointsX; x++) {
				assertEquals("y " + y + " and x " + x + " produced an error.", PathFindingTag.Target,
						cellGrid.getValue(x, y).tag);
			}
		}
	}

	/**
	 * Test method for
	 * {@link org.vadere.util.potential.FloorDiscretizer#setGridDistanceValuesForShape(CellGrid, org.vadere.util.geometry.shapes.VShape, PathFindingTag)}.
	 * Tests that in case of using a shape larger than the grid, the algorithms still works as if
	 * the shape were equally large.
	 */
	@Test
	public void testSetGridDistanceValuesForShape_halfGridHeightPlusEps() {
		shapeToDiscretize = new VRectangle(0, 0, 2.1, 1);

		FloorDiscretizer.setGridDistanceValuesForShape(cellGrid, shapeToDiscretize, PathFindingTag.Target,
				PathFindingTag.Reachable);

		// test if all the values are set to zero even only half the grid was filled (i.e. the
		// initial values are zero)
		double counter = 0;
		for (int y = 0; y < cellGrid.numPointsY; y++) {
			for (int x = 0; x < cellGrid.numPointsX; x++) {
				counter += cellGrid.getValue(x, y).potential;
			}
		}
		assertEquals(0, counter, 1e-8);
	}

	/**
	 * Test method for
	 * {@link org.vadere.util.potential.FloorDiscretizer#setGridDistanceValuesForShape(CellGrid, org.vadere.util.geometry.shapes.VShape, PathFindingTag)}.
	 * Tests that in case of using a shape slightly overlapping certain cells, the distances are
	 * computed correctly.
	 */
	@Test
	public void testSetGridDistanceValuesForShape_halfGridWidthPlusEps() {
		shapeToDiscretize = new VRectangle(0, 0, 2, 1.1);

		FloorDiscretizer.setGridDistanceValuesForShape(cellGrid, shapeToDiscretize, PathFindingTag.Target,
				PathFindingTag.Reachable);

		// test if not all the values are set to zero even only half the grid was filled (i.e. the
		// initial values are zero)
		double counter = 0;
		int reachableCounter = 0;
		int targetCounter = 0;
		for (int y = 0; y < cellGrid.numPointsY; y++) {
			for (int x = 0; x < cellGrid.numPointsX; x++) {
				counter += cellGrid.getValue(x, y).potential;
				if (cellGrid.getValue(x, y).tag == PathFindingTag.Reachable) {
					reachableCounter++;
				}
				if (cellGrid.getValue(x, y).tag == PathFindingTag.Target) {
					targetCounter++;
				}
			}
		}
		// compute the distances that should be computed by setGridDistanceValuesForShape:
		double distanceSum =
				cellGrid.getNumPointsY() * (Math.ceil(shapeToDiscretize.height) - shapeToDiscretize.height);
		assertEquals(distanceSum, counter, 1e-8);
		assertEquals(cellGrid.getNumPointsY(), reachableCounter);
		assertEquals(cellGrid.getNumPointsY() * (cellGrid.getNumPointsX() + 1) / 2, targetCounter);
	}


}
