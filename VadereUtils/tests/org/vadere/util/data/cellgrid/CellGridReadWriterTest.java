package org.vadere.util.data.cellgrid;

import org.junit.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class CellGridReadWriterTest {

	private String loadTestResource(String path){
		URL url = CellGridTest.class.getResource(path);
		try {
			return url.toURI().getPath();
		} catch (URISyntaxException e) {
			fail("TestResource not found " + path);
		}
		return "";
	}

	@Test
	public void testCSV() throws Exception {
		String pathCSV = loadTestResource("/org/vadere/util/data/cellgrid/test001.ffcache");
		CellGrid cellGrid = new CellGrid(3.0, 3.0, 1.0, new CellState(), 0.0, 0.0);

		// read in csv
		CellGridReadWriter.read(cellGrid).fromCsv(Paths.get(pathCSV).toFile());

		// write back
		CellGridReadWriter.write(cellGrid).toCsv(Paths.get(pathCSV+"back").toFile());

		// read again
		CellGrid cellGrid2 = new CellGrid(3.0, 3.0, 1.0, new CellState(), 0.0, 0.0);
		CellGridReadWriter.read(cellGrid2).fromCsv(Paths.get(pathCSV+"back").toFile());


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
		String pathBIN = loadTestResource("/org/vadere/util/data/cellgrid/test001.bincache");
		CellGrid cellGrid = new CellGrid(3.0, 3.0, 1.0, new CellState(), 0.0, 0.0);

		// read in csv
		CellGridReadWriter.read(cellGrid).fromBinary(Paths.get(pathBIN).toFile());

		// write back
		CellGridReadWriter.write(cellGrid).toBinary(Paths.get(pathBIN+"back").toFile());

		// read again
		CellGrid cellGrid2 = new CellGrid(3.0, 3.0, 1.0, new CellState(), 0.0, 0.0);
		CellGridReadWriter.read(cellGrid2).fromBinary(Paths.get(pathBIN+"back").toFile());


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