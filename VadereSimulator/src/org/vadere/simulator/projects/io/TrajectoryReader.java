package org.vadere.simulator.projects.io;

import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.FootStepProcessor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

/**
 * A TrajectoryReader is the counterpart of the {@link FootStepProcessor}.
 *
 * Output file assumptions:
 * The TrajectoryReader assumes that the first row of the output is the headline and
 * that there exist certain columns named:
 *      (id or pedestrianId) [mandatory],
 *      simTime [mandatory],
 *      startX [mandatory],
 *      startY [mandatory],
 *      endX [mandatory],
 *      endY [mandatory],
 *      targetId [optional] and
 *      groupId [optional].
 * The order of the rows (expect for the first row / header) can be arbitrary.
 * Columns has to be separated by {@link TrajectoryReader#SPLITTER} and {@link OutputFile#headerProcSep}.
 */
public class TrajectoryReader {

	private static final char SPLITTER = ' ';
	private Table dataFrame;
	private Path trajectoryFilePath;

	public TrajectoryReader(final Path trajectoryFilePath) {
		this.trajectoryFilePath = trajectoryFilePath;
	}

	public Table readFile() throws IOException {
		CsvReadOptions options = CsvReadOptions.builder(trajectoryFilePath.toFile()).separator(SPLITTER).header(true).build();
		dataFrame = Table.read().usingOptions(options);
		ColumnNames columnNames = ColumnNames.getInstance();
		if(columnNames.hasDuplicates(dataFrame)) {
			throw new IOException("The header of table " + dataFrame + " is contains duplicates which can lead to unwanted side effects");
		}
		return dataFrame;
	}
}
