package org.vadere.util.data.cellgrid;


import org.vadere.util.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CellGridConverter {
	private static Logger logger = Logger.getLogger(CellGridConverter.class);

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

			// first line is the headline
			if (!firstLine) {
				if (resolution == null) {
					resolution = width / (splitLine.length - 2);
				}

				int newTimeStep = Integer.parseInt(splitLine[0]);
				int row = Integer.parseInt(splitLine[1]);

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
}
