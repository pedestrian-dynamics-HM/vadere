package org.vadere.gui.postvisualization.model;

import org.jetbrains.annotations.NotNull;

import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;

public interface ITableTrajectory {

	Table getTrajectoryDataFrame();
	Table getAgentDataFrame();

	void setSlice(final double startX, final double endX);

	default boolean isEmpty() {
		return getTrajectoryDataFrame().isEmpty();
	}

	default Table getAgents(final double startX, final double endX) {
		return getCurrentSlice().where(getStartTime().isGreaterThanOrEqualTo(startX).and(getEndTime().isLessThanOrEqualTo(endX)));
	}

	default Table getAliveAgents(final double startX, final double endX) {
		return getCurrentSlice().where(getPedId().isIn(filterAgents(startX, endX))).where(getStartTime().isGreaterThanOrEqualTo(startX).and(getEndTime().isLessThanOrEqualTo(endX)));
	}

	default Table getAgents(final double simTimeInSec) {
		return getCurrentSlice().where(
				getStartTime().isGreaterThanOrEqualTo(simTimeInSec)
						.and(getEndTime().isLessThanOrEqualTo(simTimeInSec)))
				.sortAscendingOn(getColumnName(getPedIdCol()));
	}

	default Table getAgent(final double simTimeInSec, final int pedId) {
		return getCurrentSlice().where(
				getStartTime().isGreaterThanOrEqualTo(simTimeInSec)
						.and(getEndTime().isLessThanOrEqualTo(simTimeInSec))
						.and(getPedId().isEqualTo(pedId)));
	}

	default Integer[] filterAgents(final double startTime, final double endTime) {
		return getCurrentSlice()
				.where(getBirthTime().isGreaterThanOrEqualTo(startTime).and(getDeathTime().isLessThanOrEqualTo(endTime)))
				.intColumn(getPedIdColADF())
				.asObjectArray();
	}

	int getPedIdCol();


	int getPedIdColADF();

	Table getCurrentSlice();

	double getMaxEndTime();

	double getMinStartTime();

	default String getColumnName(final int colIndex) {
		return getTrajectoryDataFrame().columnNames().get(colIndex);
	}

	default IntColumn getPedId(@NotNull final Table table) {
		return table.intColumn(getPedIdCol());
	}

	DoubleColumn getStartX(@NotNull final Table table);

	DoubleColumn getStartY(@NotNull final Table table);

	DoubleColumn getEndX(@NotNull final Table table);

	DoubleColumn getEndY(@NotNull final Table table);

	DoubleColumn getStartTime(@NotNull final Table table);

	DoubleColumn getEndTime(@NotNull final Table table);

	DoubleColumn getBirthTime();

	DoubleColumn getDeathTime();

	double getBirthTime(final int pedId);

	double getDeathTime(final int pedId);

	default IntColumn getPedId() {
		return getPedId(getCurrentSlice());
	}

	default DoubleColumn getStartX() {
		return getStartX(getCurrentSlice());
	}

	default DoubleColumn getStartY() {
		return getStartY(getCurrentSlice());
	}

	default DoubleColumn getEndX() {
		return getEndX(getCurrentSlice());
	}

	default DoubleColumn getEndY() {
		return getEndY(getCurrentSlice());
	}

	default DoubleColumn getStartTime() {
		return getStartTime(getCurrentSlice());
	}

	default DoubleColumn getEndTime() {
		return getEndTime(getCurrentSlice());
	}
}
