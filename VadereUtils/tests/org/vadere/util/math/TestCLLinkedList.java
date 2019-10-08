package org.vadere.util.math;


import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.CLLinkedCell;
import org.vadere.util.opencl.OpenCLException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.lwjgl.opencl.CL10.CL_DEVICE_TYPE_ALL;
import static org.junit.Assert.assertEquals;

/**
 * @author Benedikt Zoennchen
 */
public class TestCLLinkedList {

	private static Logger logger = Logger.getLogger(TestCLLinkedList.class);

	private static Random random = new Random(0);

	@Before
	public void setUp() throws Exception {}

	@Test
	public void testCalcHashSmall() throws OpenCLException {
		int size = 8;
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 0.6);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(0.5 + random.nextFloat() * 9,0.5 + random.nextFloat() * 9));
		}
		int[] hasehs = clUniformHashedGrid.calcHashes(positions);

		assertEquals(hasehs.length, positions.size());

		//logger.info("number of cells = " + clUniformHashedGrid.getGridSize()[0] * clUniformHashedGrid.getGridSize()[1]);
		for(int i = 0; i < hasehs.length; i++) {
			int hash = getGridHash(getGridPosition(positions.get(i), clUniformHashedGrid.getCellSize(), clUniformHashedGrid.getWorldOrign()), clUniformHashedGrid.getGridSize());
			assertEquals(hasehs[i], hash);
			//logger.info("hash = " + hash);
		}
	}

	@Test
	public void testCalcHashLarge() throws OpenCLException {
		int size = 32768; // 2^15
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 0.6);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(0.5 + random.nextFloat() * 9,0.5 + random.nextFloat() * 9));
		}
		int[] hasehs = clUniformHashedGrid.calcHashes(positions);

		assertEquals(hasehs.length, positions.size());

		//logger.info("number of cells = " + clUniformHashedGrid.getGridSize()[0] * clUniformHashedGrid.getGridSize()[1]);
		for(int i = 0; i < hasehs.length; i++) {
			int hash = getGridHash(getGridPosition(positions.get(i), clUniformHashedGrid.getCellSize(), clUniformHashedGrid.getWorldOrign()), clUniformHashedGrid.getGridSize());
			assertEquals(hasehs[i], hash);
			//logger.info("hash = " + hash);
		}
	}

	@Test
	public void testCalcAndSortHashSmall() throws OpenCLException {
		int size = 8;
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 1);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(0.5 + random.nextFloat() * 9,0.5 + random.nextFloat() * 9));
		}
		int[] hasehs = clUniformHashedGrid.calcSortedHashes(positions);

		assertEquals(hasehs.length, positions.size());

		int[] expectedHashes = new int[positions.size()];
		for(int i = 0; i < hasehs.length; i++) {
			int hash = getGridHash(getGridPosition(positions.get(i), clUniformHashedGrid.getCellSize(), clUniformHashedGrid.getWorldOrign()), clUniformHashedGrid.getGridSize());
			expectedHashes[i] = hash;
		}
		Arrays.sort(expectedHashes);

		for(int i = 0; i < hasehs.length; i++) {
			assertEquals(hasehs[i], expectedHashes[i]);
		}
	}

	@Test
	public void testCalcAndSortHashLarge() throws OpenCLException {
		int size = 32768; // 2^15
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 0.6);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(0.5 + random.nextFloat() * 9,0.5 + random.nextFloat() * 9));
		}
		int[] hashes = clUniformHashedGrid.calcSortedHashes(positions);

		assertEquals(hashes.length, positions.size());

		int[] expectedHashes = new int[positions.size()];
		for(int i = 0; i < hashes.length; i++) {
			int hash = getGridHash(getGridPosition(positions.get(i), clUniformHashedGrid.getCellSize(), clUniformHashedGrid.getWorldOrign()), clUniformHashedGrid.getGridSize());
			expectedHashes[i] = hash;
		}
		Arrays.sort(expectedHashes);

		for(int i = 0; i < hashes.length; i++) {
			assertEquals(hashes[i], expectedHashes[i]);
		}
	}

	@Test
	public void testGridCellSmall() throws OpenCLException {
		testGridCell(CL_DEVICE_TYPE_ALL, 8);
	}

	@Test
	public void testGridCellMedium() throws OpenCLException {
		testGridCell(CL_DEVICE_TYPE_ALL, 1024);
	}

	@Ignore
	@Test
	public void testGridCellLarge() throws OpenCLException {
		testGridCell(CL_DEVICE_TYPE_ALL, 32768);
	}

	private void testGridCell(final int deviceType, int size) throws OpenCLException {
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 0.6, deviceType);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(0.5 + random.nextFloat() * 9,0.5 + random.nextFloat() * 9));
		}

		long ms = System.currentTimeMillis();
		CLLinkedCell.LinkedCell gridCells = clUniformHashedGrid.calcLinkedCell(positions);
		long runtime = System.currentTimeMillis() - ms;
		logger.infof("testGridCellSmall required " + runtime + " [ms]");

		equalPositions(gridCells);
		int numberOfCells = clUniformHashedGrid.getGridSize()[0] * clUniformHashedGrid.getGridSize()[1];
		int sum = 0;
		for(int cell = 0; cell < numberOfCells; cell++) {
			int cellStart = gridCells.cellStarts[cell];
			int cellEnd = gridCells.cellEnds[cell];

			for(int i = cellStart; i < cellEnd; i++) {
				VPoint point = new VPoint(gridCells.reorderedPositions[i*2], gridCells.reorderedPositions[i*2+1]);
				int[] gridPosition = getGridPosition(point, clUniformHashedGrid.getCellSize(), clUniformHashedGrid.getWorldOrign());
				int gridHash = getGridHash(gridPosition, clUniformHashedGrid.getGridSize());
				sum++;
				assertEquals(gridHash, cell);
			}
		}

		assertEquals(size, sum);
	}

	private void equalPositions(@NotNull final CLLinkedCell.LinkedCell linkedCell) {
		assertEquals(linkedCell.positions.length, linkedCell.reorderedPositions.length);
		for(int i = 0; i < linkedCell.positions.length / 2; i++) {
			float x = linkedCell.positions[2*i];
			float y = linkedCell.positions[2*i+1];
			boolean found = false;

			for(int j = 0; j < linkedCell.reorderedPositions.length / 2; j++) {
				float xR = linkedCell.reorderedPositions[2*j];
				float yR = linkedCell.reorderedPositions[2*j+1];
				if(x == xR && y == yR) {
					found = true;
					break;
				}
			}

			if(!found) {
				assert false : "position (" + x + "," + y + ") not found";
			}

		}
	}

	/**
	 * Helper to compute the hash values, see OpenCL code in Particles.cl
	 */
	private static int umad(int a, int b, int c) {
		return  (a * b) + c;
	}

	/**
	 * Computes the grid position of a real world position, see OpenCL code in Particles.cl
	 */
	private static int[] getGridPosition(final VPoint p, float cellSize, final VPoint worldOrign) {
		int[] gridPos = new int[2];
		gridPos[0] = (int)Math.floor( (p.getX()-worldOrign.getX()) / cellSize );
		gridPos[1] = (int)Math.floor( (p.getY()-worldOrign.getY()) / cellSize );
		return gridPos;
	}

	/**
	 * Computes the hash value of a grid position, see OpenCL code in Particles.cl
	 */
	private static int getGridHash(final int[] gridPos, int[] gridSize) {
		//gridPos[0] = gridPos[0] & (gridSize[0] - 1);
		//gridPos[1] = gridPos[1] & (gridSize[1] - 1);
		return umad(gridSize[0], gridPos[1], gridPos[0]);
	}
}
