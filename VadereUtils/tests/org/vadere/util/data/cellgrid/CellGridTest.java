package org.vadere.util.data.cellgrid;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Random;

import tech.tablesaw.api.Table;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class CellGridTest {

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
	public void loadCache(){
		String path = loadTestResource("/org/vadere/util/data/cellgrid/test001.ffcache");
		Table t = null;
		try {
			t = Table.read().csv(Paths.get(path).toFile());
		} catch (IOException e) {
			fail("Test file not found");
			return;

		}
		CellGrid cellGrid = new CellGrid(3.0, 3.0, 1.0, new CellState(), 0.0, 0.0);
		assertThat(t.rowCount(), equalTo(cellGrid.numPointsX*cellGrid.numPointsY));

		cellGrid.loadFromTable(t);
		compare(t, cellGrid);
	}

	@Test
	public void saveGridToCache(){
		Random rnd = new Random(0);
		CellGrid cellGrid = new CellGrid(3.0, 3.0, 1.0, new CellState(), 0.0, 0.0);
		int maxPathFindingTag = PathFindingTag.values().length;

		// set random data to CellGrid
		for (int row = 0; row < cellGrid.numPointsY; row++) {
			for (int col = 0; col < cellGrid.numPointsX; col++) {
				cellGrid.values[col][row] =
						new CellState(rnd.nextDouble(),
								PathFindingTag.values()[rnd.nextInt(maxPathFindingTag)]);
			}
		}

		compare(cellGrid.asTable(), cellGrid);
	}

	private void compare(Table t, CellGrid cellGrid){
		for (int row = 0; row < cellGrid.numPointsY; row++) {
			for (int col = 0; col < cellGrid.numPointsX; col++) {
				CellState state = cellGrid.values[col][row];
				Table f = t.where(
						t.intColumn("x").isEqualTo(col)
								.and(t.intColumn("y").isEqualTo(row))
				);
				assertThat(f.rowCount(), equalTo(1));
				assertThat(f.column("value").get(0), equalTo(state.potential));
				assertThat(f.column("tag").get(0), equalTo(state.tag.name()));
			}
		}
	}

}