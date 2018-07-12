package org.vadere.util.math;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.opencl.CLUniformHashedGrid;
import org.vadere.util.opencl.OpenCLException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Benedikt Zoennchen
 */
public class TestCellGridSort {

	private static Logger logger = LogManager.getLogger(TestConvolution.class);

	private static Random random = new Random();

	@Before
	public void setUp() throws Exception {}

	@Test
	public void testCalcHash() throws IOException, OpenCLException {
		CLUniformHashedGrid clUniformHashedGrid = new CLUniformHashedGrid(1024, new VRectangle(0, 0, 10, 10), 1);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < 1024; i++) {
			positions.add(new VPoint(random.nextDouble() * 10,random.nextDouble() * 10));
		}
		int[] hasehs = clUniformHashedGrid.calcHashes(positions);

		assertEquals(hasehs.length, positions.size());

		for(int i = 0; i < hasehs.length; i++) {
			int hash = getGridHash(getGridPosition(positions.get(i), clUniformHashedGrid.getCellSize(), clUniformHashedGrid.getWorldOrign()), clUniformHashedGrid.getGridSize());
			assertEquals(hasehs[i], hash);
		}
	}

	@Test
	public void testCalcAndSortHash() throws IOException, OpenCLException {
		CLUniformHashedGrid clUniformHashedGrid = new CLUniformHashedGrid(1024, new VRectangle(0, 0, 10, 10), 1);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < 1024; i++) {
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
	public void testGridCell() throws IOException, OpenCLException {
		CLUniformHashedGrid clUniformHashedGrid = new CLUniformHashedGrid(1024, new VRectangle(0, 0, 10, 10), 1);
		ArrayList<VPoint> positions = new ArrayList<>();
		for(int i = 0; i < 1024; i++) {
			positions.add(new VPoint(random.nextDouble() * 10,random.nextDouble() * 10));
		}
		CLUniformHashedGrid.GridCells gridCells = clUniformHashedGrid.calcPositionsInCell(positions);

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
		return umad(umad(1, gridSize[1], gridPos[1]), gridSize[0], gridPos[0]);
	}
}
