package org.vadere.gui.postvisualization.utils;


import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.data.cellgrid.CellGridConverter;
import org.vadere.util.data.cellgrid.CellState;
import org.vadere.util.data.cellgrid.PathFindingTag;
import org.vadere.util.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class PotentialFieldContainer {
	private static Logger logger = Logger.getLogger(PotentialFieldContainer.class);

	private List<CellGrid> potentialFields;

	private final boolean onDemand;

	private final File file;

	private final double width;

	private final double height;

	private int step;

	private int readStep;

	private BufferedReader reader;

	private CellGrid grid;

	private boolean firstLine;

	public PotentialFieldContainer(final File file, final double width, final double height, final boolean onDemand) {
		this.onDemand = onDemand;
		this.file = file;
		this.width = width;
		this.height = height;

		try {
			reset();
			// never happen!
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void reset() throws IOException {

		if (reader != null) {
			reader.close();
		}

		this.step = -1;
		this.readStep = -1;
		this.firstLine = true;
		this.grid = null;
		this.potentialFields = null;

		if (!onDemand) {
			reader = null;
		} else {
			reader = new BufferedReader(new FileReader(file));
		}
	}

	public PotentialFieldContainer(final File file, final double width, final double height) {
		this(file, width, height, false);
	}

	public CellGrid getPotentialField(final int step) throws IOException {
		if (!onDemand) {
			if (potentialFields == null) {
				potentialFields = CellGridConverter.fromOutputProcessorFile(file, width, height);
			}

			if (potentialFields.isEmpty()) {
				return new CellGrid(0, 0, 0, new CellState());
			} else {
				return potentialFields.get(Math.min(step - 1, potentialFields.size() - 1));
			}
		} else {
			if (this.step == step || readStep >= step) {
				return grid;
			} else {
				if (this.step > step) {
					reset();
				}
				return getPotentialByStep(step, " ");
			}
		}
	}

	public void clear() throws IOException {
		if (reader != null) {
			reader.close();
		}
	}

	public CellGrid getPotentialByStep(final int requestedStep, final String seperator) throws IOException {

		String line = null;
		Double resolution = null;
		int y = -1;

		while ((line = reader.readLine()) != null) {
			y++;
			String[] splitLine = line.split(seperator);
			// first line is the headline
			if (!firstLine) {
				if (resolution == null) {
					resolution = width / (splitLine.length - 2);
				}

				readStep = Integer.parseInt(splitLine[0]);
				int row = Integer.parseInt(splitLine[1]);

				if (readStep - 1 == requestedStep) {
					return grid;
				} else if (readStep - 1 > requestedStep) {
					logger.warn("floor field for step " + (readStep - 1) + " does not exist");
					return grid;
				}

				if (readStep != step) {
					step = readStep;
					y = 0;
					grid = new CellGrid(width, height, resolution, new CellState());
				}

				for (int x = 2; x < splitLine.length; x++) {
					CellState state = new CellState(Double.parseDouble(splitLine[x]), PathFindingTag.Undefined);
					if (y < grid.getNumPointsY() && (x - 2) < grid.getNumPointsX()) {
						grid.setValue(x - 2, y, state);
					} else {
						logger.warn("Index topographyError " + "y=" + y + " rows=" + grid.getNumPointsY() + " x=" + x + " cols="
								+ grid.getNumPointsX());
					}
				}
			} else {
				firstLine = false;
			}
		}

		return grid;
	}
}
