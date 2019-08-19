package org.vadere.util.data.cellgrid;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;

import org.vadere.util.io.IDataReader;
import org.vadere.util.io.IDataWriter;
import org.vadere.util.logging.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

public class CellGridReadWriter implements IDataWriter, IDataReader<CellGrid> {

	private static Logger logger = Logger.getLogger(CellGridReadWriter.class);

	private CellGrid cellGrid;

	public static IDataWriter write(CellGrid cellGrid){
		return new CellGridReadWriter(cellGrid);
	}

	public static IDataReader<CellGrid> read(CellGrid cellGrid){
		return new CellGridReadWriter(cellGrid);
	}

	public static IDataReader<CellGrid> read(double width, double height, double resolution, double xMin, double yMin){
		return new CellGridReadWriter(new CellGrid(width, height, resolution, new CellState(), xMin, yMin));
	}

	private CellGridReadWriter(CellGrid cellGrid) {
		this.cellGrid = cellGrid;
	}


	@Override
	public CellGrid fromTextFile(File file) throws IOException {
		return readCsv(Table.read().csv(file));
	}

	@Override
	public CellGrid fromTextFile(InputStream inputStream) throws IOException {
		return readCsv(Table.read().csv(inputStream));
	}

	private CellGrid readCsv(Table table) {
		int xDim;
		int yDim;
		try {
			xDim = (int) table.intColumn("x").max() + 1;
			yDim = (int) table.intColumn("y").max() + 1;
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Cannot read from txt file. Expected column 'x' or 'y' does not exist", e);
		}

		assert xDim == cellGrid.getNumPointsX();
		assert yDim == cellGrid.getNumPointsY();
		CellState[][] values = cellGrid.values;
		for (Row r : table){
			values[r.getInt("x")][r.getInt("y")].potential = r.getDouble("value");
			values[r.getInt("x")][r.getInt("y")].tag =
					PathFindingTag.valueOf(r.getString("tag"));
		}

		return cellGrid;
	}



	@Override
	public void toTextFile(File file) throws IOException {
		file.getAbsoluteFile().getParentFile().mkdirs();
		CellState[][] values = cellGrid.values;

		PrintWriter stream = new PrintWriter(
				new FastBufferedOutputStream(new FileOutputStream(file)));
		stream.write("x,y,value,tag\n");
		for (int row = 0; row < cellGrid.getNumPointsY(); row++) {
			for (int col = 0; col < cellGrid.getNumPointsX(); col++) {
				stream.write(Integer.toString(col));
				stream.write(",");
				stream.write(Integer.toString(row));
				stream.write(",");
				stream.write(values[col][row].potential.toString());
				stream.write(",");
				stream.write(values[col][row].tag.name());
				stream.write("\n");
			}
		}
		stream.flush();
	}

	@Override
	public CellGrid fromBinary(File file) throws IOException {
		DataInputStream stream = new DataInputStream(
				new FastBufferedInputStream(new FileInputStream(file))
		);
		return fromBinary(stream);
	}

	@Override
	public CellGrid fromBinary(DataInputStream stream) throws IOException {
		int xDim;
		int yDim;
		int columns;
		try{
			xDim = stream.readInt();
			yDim = stream.readInt();
			columns = stream.readInt();
		} catch (EOFException eof){
			throw new IllegalArgumentException("Stream ended to soon.");
		}

		assert xDim == cellGrid.getNumPointsX();
		assert yDim == cellGrid.getNumPointsY();
		CellState[][] values = cellGrid.values;

		int lines = 0;
		int maxLines = xDim * yDim;
		while (lines < maxLines){

			try {
				int col = stream.readInt();
				int row = stream.readInt();
				double val = stream.readDouble();
				PathFindingTag tag = PathFindingTag.valueOf(stream.readInt());
				values[col][row].potential = val;
				values[col][row].tag = tag;
				lines++;
			} catch (EOFException eof){
				throw new IllegalArgumentException("Stream ended to soon. Expected " +
						maxLines + "liens but only received " + lines + " lines.");
			}
		}
		return cellGrid;
	}

	@Override
	public void toBinary(File file) throws IOException {
		file.getAbsoluteFile().getParentFile().mkdirs();
		CellState[][] values = cellGrid.values;

		DataOutputStream stream = new DataOutputStream(
				new FastBufferedOutputStream(new FileOutputStream(file))
		);

		stream.writeInt(cellGrid.getNumPointsX());
		stream.writeInt(cellGrid.getNumPointsY());
		stream.writeInt(4); // number of columns for dataframe
		for (int row = 0; row < cellGrid.getNumPointsY(); row++) {
			for (int col = 0; col < cellGrid.getNumPointsX(); col++) {
				stream.writeInt(col);
				stream.writeInt(row);
				stream.writeDouble(values[col][row].potential);
				stream.writeInt(values[col][row].tag.ordinal());
			}
		}
		stream.flush();
	}
}
