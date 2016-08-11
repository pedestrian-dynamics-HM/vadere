package org.vadere.util.potential;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.data.Table;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CellGridConverter {
	private static Logger logger = LogManager.getLogger(CellGridConverter.class);
	private CellGrid grid;

	public CellGridConverter(final CellGrid grid) {
		this.grid = grid;
	}

	public static List<CellGrid> fromOutputProcessorFile(final File file, final double width, final double height)
			throws IOException {
		return fromOutputProcessorFile(file, width, height, " ");
	}

	public static List<CellGrid> fromOutputProcessorFile(final File file, final double width, final double height,
			final String seperator) throws IOException {
		List<CellGrid> gridPerStep = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;

		Double resolution = null;
		CellGrid grid = null;
		int timeStep = -1;
		int y = -1;
		boolean firstLine = true;


		while ((line = reader.readLine()) != null) {
			y++;
			String[] splitLine = line.split(seperator);
			if (resolution == null) {
				resolution = width / (splitLine.length - 2);
			}

			// first line is the headline
			if (!firstLine) {
				int newTimeStep = Integer.parseInt(splitLine[0]);
				double time = Double.parseDouble(splitLine[1]);

				if (newTimeStep != timeStep) {
					timeStep = newTimeStep;
					y = 0;
					grid = new CellGrid(width, height, resolution, new CellState());
					gridPerStep.add(grid);
				}

				for (int x = 2; x < splitLine.length; x++) {
					CellState state = new CellState(Double.parseDouble(splitLine[x]), PathFindingTag.Undefined);
					grid.setValue(x - 2, y, state);
				}

				if (timeStep != gridPerStep.size()) {
					logger.warn("wrong time step   in List<CellGrid> " + timeStep + " != " + gridPerStep.size());
				}
			} else {
				firstLine = false;
			}
		}

		reader.close();

		return gridPerStep;
	}

	public Table toTable() {
		return toTable(new String[] {}, new String[] {});
	}

	public Table toTable(final String[] additionalColumns, final Object[] additionalValues) {
		if (additionalColumns.length != additionalValues.length) {
			throw new IllegalArgumentException(additionalColumns.length + " != " + additionalValues.length);
		}

		String[] columnNames = new String[grid.numPointsX + additionalColumns.length];

		for (int i = 0; i < additionalColumns.length; i++) {
			columnNames[i] = additionalColumns[i];
		}

		for (int x = 0; x < grid.numPointsX; x++) {
			columnNames[additionalColumns.length + x] = String.valueOf("x" + (x + 1));
		}

		Table gridTable = new Table(columnNames);

		for (int y = 0; y < grid.numPointsY; y++) {
			gridTable.addRow();

			for (int i = 0; i < additionalColumns.length; i++) {
				gridTable.addColumnEntry(additionalColumns[i], additionalValues[i]);
			}

			for (int x = 0; x < grid.numPointsX; x++) {
				gridTable.addColumnEntry(columnNames[additionalColumns.length + x], grid.values[x][y].potential);
			}
		}

		return gridTable;
	}
}
