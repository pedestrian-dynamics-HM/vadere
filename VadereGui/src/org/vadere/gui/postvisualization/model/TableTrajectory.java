package org.vadere.gui.postvisualization.model;

import org.jetbrains.annotations.NotNull;

import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import static tech.tablesaw.aggregate.AggregateFunctions.max;
import static tech.tablesaw.aggregate.AggregateFunctions.min;

public class TableTrajectory implements ITableTrajectory {
	private final Table trajectoryDataFrame;
	private Table currentSlice;
	private final Table agentDataFrame;

	private final double startTime;
	private final double endTime;

	// columns
	public final int pedIdCol = -1;
	public final int startXCol = -1;
	public final int startYCol = -1;
	public final int endXCol = -1;
	public final int endYCol = -1;
	public final int startTimeCol = -1;
	public final int endTimeCol = -1;

	public static final int agentDFPedIdCol = 0;
	public static final int birthTimeCol = 1;
	public static final int deathTimeCol = 2;
	public static final String birthTimeColName = "birthTime";
	public static final String deathTimeColName = "deathTime";

	public TableTrajectory(@NotNull final Table dataFrame) {
		this.trajectoryDataFrame = dataFrame;
		this.currentSlice = trajectoryDataFrame;
		this.agentDataFrame = generateAgentDataFrame();
		this.startTime = agentDataFrame.summarize(birthTimeColName, min).apply().doubleColumn(0).get(0);
		this.endTime = agentDataFrame.summarize(deathTimeColName, max).apply().doubleColumn(0).get(0);
	}

	@Override
	public Table getTrajectoryDataFrame() {
		return trajectoryDataFrame;
	}

	@Override
	public Table getAgentDataFrame() {
		return agentDataFrame;
	}

	private Row getAgentDataFrameRow(final int pedId) {
		return agentDataFrame.rows(pedId).iterator().next();
	}

	@Override
	public void setSlice(final double startX, final double endX) {
		currentSlice = trajectoryDataFrame.where(trajectoryDataFrame.doubleColumn(startTimeCol).isGreaterThanOrEqualTo(startX).and(trajectoryDataFrame.doubleColumn(endTimeCol).isLessThanOrEqualTo(endX)));
	}

	@Override
	public int getPedIdCol() {
		return pedIdCol;
	}

	@Override
	public int getPedIdColADF() {
		return agentDFPedIdCol;
	}

	@Override
	public Table getCurrentSlice() {
		return currentSlice;
	}

	@Override
	public double getMaxEndTime() {
		return endTime;
	}

	@Override
	public double getMinStartTime() {
		return startTime;
	}

	private Table generateAgentDataFrame() {
		Table agentDataFrame = trajectoryDataFrame.summarize(getStartTime(), getEndTime(), min, max).by(getColumnName(pedIdCol));
		agentDataFrame.column(1).setName(birthTimeColName);
		agentDataFrame.column(4).setName(deathTimeColName);
		agentDataFrame.removeColumns(2, 3);
		return agentDataFrame.sortAscendingOn(getColumnName(pedIdCol));
	}

	/**
	 * Searches for the correct column indices.
	 */
	private void searchCols() {

	}

	public IntColumn getPedId(@NotNull final Table table) {
		return table.intColumn(pedIdCol);
	}

	public DoubleColumn getStartX(@NotNull final Table table) {
		return table.doubleColumn(startXCol);
	}

	public DoubleColumn getStartY(@NotNull final Table table) {
		return table.doubleColumn(startYCol);
	}

	public DoubleColumn getEndX(@NotNull final Table table) {
		return table.doubleColumn(endXCol);
	}

	public DoubleColumn getEndY(@NotNull final Table table) {
		return table.doubleColumn(endYCol);
	}

	public DoubleColumn getStartTime(@NotNull final Table table) {
		return table.doubleColumn(startTimeCol);
	}

	public DoubleColumn getEndTime(@NotNull final Table table) {
		return table.doubleColumn(endTimeCol);
	}

	public DoubleColumn getBirthTime() {
		return agentDataFrame.doubleColumn(birthTimeCol);
	}

	public DoubleColumn getDeathTime() {
		return agentDataFrame.doubleColumn(deathTimeCol);
	}

	public double getBirthTime(final int pedId) {
		return agentDataFrame.where(agentDataFrame.doubleColumn(pedIdCol).isEqualTo(pedId)).doubleColumn(birthTimeCol).get(0);
	}

	public double getDeathTime(final int pedId) {
		return agentDataFrame.where(agentDataFrame.doubleColumn(pedIdCol).isEqualTo(pedId)).doubleColumn(deathTimeCol).get(0);
	}
}
