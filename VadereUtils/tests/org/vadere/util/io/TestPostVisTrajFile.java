package org.vadere.util.io;

import junit.framework.TestCase;

import org.junit.Test;

import java.io.InputStream;

import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import static tech.tablesaw.aggregate.AggregateFunctions.*;

public class TestPostVisTrajFile extends TestCase {

	private Table tableV1;
	private Table tableV2;

	@Override
	protected void setUp() throws Exception {
		InputStream inputStream = TestPostVisTrajFile.class.getResourceAsStream("/postvis.traj");
		CsvReadOptions options = CsvReadOptions.builder(inputStream).separator(' ').header(true).build();
		tableV2 = Table.read().usingOptions(options);

		inputStream = TestPostVisTrajFile.class.getResourceAsStream("/postvis.trajectories");
		options = CsvReadOptions.builder(inputStream).separator(' ').header(true).build();
		tableV1 = Table.read().usingOptions(options);
	}

	@Test
	public void testGrouping() {
		Table maxTable = tableV2.summarize("simTime", max).by("pedestrianId");
		Table minTable = tableV2.summarize("simTime", min).by("pedestrianId");
		Table minMaxTable = tableV2.summarize("simTime", "endTime-PID6", min, max).by(tableV2.columnNames().get(0));
		minMaxTable.column(1).setName("birthTime");
		minMaxTable.column(4).setName("deathTime");
		minMaxTable.removeColumns(2, 3);
		minMaxTable = minMaxTable.sortAscendingOn("pedestrianId");
		System.out.println(maxTable);
		System.out.println(minTable);
		System.out.println(minMaxTable);
	}

	// timeStep pedestrianId x-PID1 y-PID1
	@Test
	public void testTransform() {
		System.out.println(tableV1.sortAscendingOn("pedestrianId", "timeStep"));
	}
}
