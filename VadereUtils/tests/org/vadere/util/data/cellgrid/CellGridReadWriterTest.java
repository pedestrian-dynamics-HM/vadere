package org.vadere.util.data.cellgrid;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class CellGridReadWriterTest {

	private File loadTestResource(String path){
		URL resource1 = CellGridTest.class.getResource(path);
		if (resource1 == null){
			Assert.fail("Resource not found: " + path);
		}
		return new File(resource1.getFile());
	}

	@Test
	public void testCSV() throws Exception {
		File pathCSV = loadTestResource("/org/vadere/util/data/cellgrid/test001.ffcache");
		CellGrid cellGrid = new CellGrid(3.0, 3.0, 1.0, new CellState(), 0.0, 0.0);

		// read in csv
		CellGridReadWriter.read(cellGrid).fromTextFile(pathCSV);

		// write back
		CellGridReadWriter.write(cellGrid).toTextFile(Paths.get(pathCSV.toString()+"back").toFile());

		// read again
		CellGrid cellGrid2 = new CellGrid(3.0, 3.0, 1.0, new CellState(), 0.0, 0.0);
		CellGridReadWriter.read(cellGrid2).fromTextFile(Paths.get(pathCSV.toString()+"back").toFile());


		// compare
		CellState[][] values1 = cellGrid.values;
		CellState[][] values2 = cellGrid2.values;
		for (int row = 0; row < cellGrid.getNumPointsY(); row++) {
			for (int col = 0; col < cellGrid.getNumPointsX(); col++) {
				assertEquals(values1[col][row], values2[col][row]);
			}
		}
	}

	@Test
	public void testBIN() throws Exception {
		File pathBIN = loadTestResource("/org/vadere/util/data/cellgrid/test001.bincache");
		CellGrid cellGrid = new CellGrid(3.0, 3.0, 1.0, new CellState(), 0.0, 0.0);

		// read in csv
		CellGridReadWriter.read(cellGrid).fromBinary(pathBIN);

		// write back
		CellGridReadWriter.write(cellGrid).toBinary(Paths.get(pathBIN.toString()+"back").toFile());

		// read again
		CellGrid cellGrid2 = new CellGrid(3.0, 3.0, 1.0, new CellState(), 0.0, 0.0);
		CellGridReadWriter.read(cellGrid2).fromBinary(Paths.get(pathBIN.toString()+"back").toFile());


		// compare
		CellState[][] values1 = cellGrid.values;
		CellState[][] values2 = cellGrid2.values;
		for (int row = 0; row < cellGrid.getNumPointsY(); row++) {
			for (int col = 0; col < cellGrid.getNumPointsX(); col++) {
				assertEquals(values1[col][row], values2[col][row]);
			}
		}
	}

}