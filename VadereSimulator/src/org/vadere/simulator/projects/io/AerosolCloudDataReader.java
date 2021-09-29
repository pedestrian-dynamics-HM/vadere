package org.vadere.simulator.projects.io;

import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Output file assumptions:
 * The {@link AerosolCloudDataReader} assumes that the first row of the output is the headline and
 * that there exist certain columns named:
 *      timeStep [mandatory],
 *      id [mandatory],
 *      pathogenLoad [mandatory],
 *      area [mandatory]
 *      vertex1X [mandatory]
 *      vertex1Y [mandatory]
 *      vertex2X [mandatory]
 *      vertex2Y [mandatory]
 * Columns have to be separated by {@link AerosolCloudDataReader#SPLITTER} and {@link OutputFile#headerProcSep}.
 */
public class AerosolCloudDataReader {

    private static final char SPLITTER = ' ';
    private Table dataFrame;
    private Path filePath;

    public AerosolCloudDataReader(final Path filePath) {
        this.filePath = filePath;
    }

    public Table readFile() throws IOException {
        CsvReadOptions options = CsvReadOptions.builder(filePath.toFile()).separator(SPLITTER).header(true).build();
        dataFrame = Table.read().usingOptions(options);
        ColumnNames columnNames = ColumnNames.getInstance();
        if(columnNames.hasDuplicates(dataFrame)) {
            throw new IOException("The header of table " + dataFrame + " contains duplicates which can lead to unwanted side effects");
        }
        return dataFrame;
    }
}
