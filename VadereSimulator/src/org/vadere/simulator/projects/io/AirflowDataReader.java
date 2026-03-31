package org.vadere.simulator.projects.io;

import tech.tablesaw.api.Table;
import tech.tablesaw.io.DataFrameReader;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.nio.file.Path;

public class AirflowDataReader {

    private static final char SPLITTER = ' ';
    private Table dataFrame;
    private Path filePath;

    public AirflowDataReader(final Path filePath) {
        this.filePath = filePath;
    }

    public Table readFile() throws IOException {
        CsvReadOptions options = CsvReadOptions.builder(filePath.toFile()).separator(SPLITTER).header(true).build();
        DataFrameReader dataFrameReader = Table.read();
        dataFrame = dataFrameReader.usingOptions(options);
        ColumnNames columnNames = ColumnNames.getInstance();

        if(columnNames.hasDuplicates(dataFrame)) {
            throw new IOException("The header of table " + dataFrame + " contains duplicates which can lead to unwanted side effects");
        }
        return dataFrame;
    }
}
