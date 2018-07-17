package org.vadere.util.math;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.opencl.CLLinkedCell;
import org.vadere.util.opencl.OpenCLException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Benedikt Zoennchen
 */
public class TestCLLinkedList {

	private static Logger logger = LogManager.getLogger(TestConvolution.class);

	private static Random random = new Random();

	@Before
	public void setUp() throws Exception {}

	@Test
	public void testCalcHashSmall() throws IOException, OpenCLException {
		int size = 8;
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 1);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(random.nextDouble() * 10,random.nextDouble() * 10));
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
	public void testCalcHashLarge() throws IOException, OpenCLException {
		int size = 32768; // 2^15
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 1);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(random.nextDouble() * 10,random.nextDouble() * 10));
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
	public void testCalcAndSortHashSmall() throws IOException, OpenCLException {
		int size = 8;
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 1);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(random.nextDouble() * 10,random.nextDouble() * 10));
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
	public void testCalcAndSortHashLarge() throws IOException, OpenCLException {
		int size = 32768; // 2^15
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 1);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(random.nextDouble() * 10,random.nextDouble() * 10));
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
	public void testGridCellSmall() throws IOException, OpenCLException {
		int size = 8;
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 1);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(random.nextDouble() * 10,random.nextDouble() * 10));
		}
		CLLinkedCell.LinkedCell gridCells = clUniformHashedGrid.calcLinkedCell(positions);
		int numberOfCells = clUniformHashedGrid.getGridSize()[0] * clUniformHashedGrid.getGridSize()[1];
		for(int cell = 0; cell < numberOfCells; cell++) {
			int cellStart = gridCells.cellStarts[cell];
			int cellEnd = gridCells.cellEnds[cell];

			for(int i = cellStart; i < cellEnd; i++) {
				VPoint point = new VPoint(gridCells.reorderedPositions[i*2], gridCells.reorderedPositions[i*2+1]);
				int[] gridPosition = getGridPosition(point, clUniformHashedGrid.getCellSize(), clUniformHashedGrid.getWorldOrign());
				int gridHash = getGridHash(gridPosition, clUniformHashedGrid.getGridSize());
				assertEquals(gridHash, cell);
			}
		}
	}

	@Test
	public void testGridCellLarge() throws IOException, OpenCLException {
		int size = 32768;
		CLLinkedCell clUniformHashedGrid = new CLLinkedCell(size, new VRectangle(0, 0, 10, 10), 1);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			positions.add(new VPoint(random.nextDouble() * 10,random.nextDouble() * 10));
		}
		CLLinkedCell.LinkedCell gridCells = clUniformHashedGrid.calcLinkedCell(positions);
		int numberOfCells = clUniformHashedGrid.getGridSize()[0] * clUniformHashedGrid.getGridSize()[1];
		for(int cell = 0; cell < numberOfCells; cell++) {
			int cellStart = gridCells.cellStarts[cell];
			int cellEnd = gridCells.cellEnds[cell];

			/*
			for(int i = cellStart; i < cellEnd; i++) {
				VPoint point = new VPoint(gridCells.reorderedPositions[i*2], gridCells.reorderedPositions[i*2+1]);
				int[] gridPosition = getGridPosition(point, clUniformHashedGrid.getCellSize(), clUniformHashedGrid.getWorldOrign());
				int gridHash = getGridHash(gridPosition, clUniformHashedGrid.getGridSize());
				assertEquals(gridHash, cell);
			}
			*/
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
		gridPos[0] = gridPos[0] & (gridSize[0] - 1);
		gridPos[1] = gridPos[1] & (gridSize[1] - 1);
		return umad(gridSize[0], gridPos[1], gridPos[0]);
	}
}
