package org.vadere.gui.postvisualization.model;

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.io.ColumnNames;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.GroupMembership;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.StimulusFactory;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Random;

import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.table.Rows;

import static tech.tablesaw.aggregate.AggregateFunctions.*;

/**
 * The {@link TableTrajectoryFootStep}
 */
public class TableTrajectoryFootStep {

	private final Table trajectoryDataFrame;

	private Table currentSlice;

	/**
	 * This table contains all constant agent information such as the birth and death time.
	 */
	private final Table agentDataFrame;

	private final double startTime;
	private final double endTime;

	// columns, TODO: this is hard coded!
	public final int pedIdCol;
	public final int startXCol;
	public final int startYCol;
	public final int endXCol;
	public final int endYCol;
	public final int startTimeCol;
	public final int endTimeCol;

	public final int targetIdCol;
	public final int groupIdCol;
	public final int groupSizeCol;

	public final int mostImportantStimulusCol;
	public final int selfCategoryCol;
	public final int groupMembershipCol;
	public final int informationStateCol;
	public final int isInfectiousCol;
	public final int degreeOfExposureCol;

	public static final int agentDFPedIdCol = 0;
	public static final int birthTimeCol = 1;
	public static final int deathTimeCol = 2;
	public static final String birthTimeColName = "birthTime";
	public static final String deathTimeColName = "deathTime";

	/**
	 * Default constructor.
	 *
	 * @param dataFrame the whole table containing all trajectories of all agents for all times
	 */
	public TableTrajectoryFootStep(@NotNull final Table dataFrame) {
		// get all ids of all columns
		// 1. mandatory columns:
		ColumnNames columnNames = ColumnNames.getInstance();
		pedIdCol = columnNames.getPedestrianIdCol(dataFrame);
		startTimeCol = columnNames.getStartTimeCol(dataFrame);
		endTimeCol = columnNames.getEndTimeCol(dataFrame);
		startXCol = columnNames.getStartXCol(dataFrame);
		startYCol = columnNames.getStartYCol(dataFrame);
		endXCol = columnNames.getEndXCol(dataFrame);
		endYCol = columnNames.getEndYCol(dataFrame);

		// optional columns:
		targetIdCol = columnNames.getTargetIdCol(dataFrame);
		groupIdCol = columnNames.getGroupIdCol(dataFrame);
		groupSizeCol = columnNames.getGroupSizeCol(dataFrame);
		mostImportantStimulusCol = columnNames.getMostImportantStimulusCol(dataFrame);
		selfCategoryCol = columnNames.getSelfCategoryCol(dataFrame);
		groupMembershipCol = columnNames.getGroupMembershipCol(dataFrame);
		informationStateCol = columnNames.getInformationStateCol(dataFrame);
		isInfectiousCol = columnNames.getIsInfectiousCol(dataFrame);
		degreeOfExposureCol = columnNames.getDegreeOfExposureCol(dataFrame);


		this.trajectoryDataFrame = dataFrame;
		this.currentSlice = trajectoryDataFrame;
		this.agentDataFrame = generateAgentDataFrame();

		if(!isEmpty()) {
			this.startTime = agentDataFrame.summarize(birthTimeColName, min).apply().doubleColumn(0).get(0);
			this.endTime = agentDataFrame.summarize(deathTimeColName, max).apply().doubleColumn(0).get(0);
		} else {
			this.startTime = 0.0;
			this.endTime = 0.0;
		}
	}

	public Table getAgentDataFrame() {
		return agentDataFrame;
	}

	protected Agent toAgent(@NotNull final Row row, @NotNull final AttributesAgent attributesAgent, final double time) {
		int pedId = row.getInt(pedIdCol);
		double startTime = row.getDouble(startTimeCol);
		double endTime = row.getDouble(endTimeCol);
		double startX = row.getDouble(startXCol);
		double startY = row.getDouble(startYCol);
		double endX = row.getDouble(endXCol);
		double endY = row.getDouble(endYCol);

		assert time <= endTime && time >= startTime;

		VPoint position = FootStep.interpolateFootStep(startX, startY, endX, endY, startTime, endTime, time);

		Pedestrian pedestrian = new Pedestrian(new AttributesAgent(attributesAgent, pedId), new Random());
		pedestrian.setPosition(position);

		// optional properties:
		if(targetIdCol != ColumnNames.NOT_SET_COLUMN_INDEX_IDENTIFIER) {
			int targetId = row.getInt(targetIdCol);
			pedestrian.getTargets().add(targetId);
		}

		if(groupIdCol != ColumnNames.NOT_SET_COLUMN_INDEX_IDENTIFIER) {
			int groupId = row.getInt(groupIdCol);
			pedestrian.getGroupIds().add(groupId);
		}

		if(groupSizeCol != ColumnNames.NOT_SET_COLUMN_INDEX_IDENTIFIER) {
			int groupSize = row.getInt(groupSizeCol);
			pedestrian.getGroupSizes().add(groupSize);
		}

		if(mostImportantStimulusCol != ColumnNames.NOT_SET_COLUMN_INDEX_IDENTIFIER) {
			String mostImportantStimulusClassName = row.getString(mostImportantStimulusCol);
			Stimulus stimulus = StimulusFactory.stringToStimulus(mostImportantStimulusClassName);
			pedestrian.setMostImportantStimulus(stimulus);
		}

		if(selfCategoryCol != ColumnNames.NOT_SET_COLUMN_INDEX_IDENTIFIER) {
			String selfCategoryEnumName = row.getString(selfCategoryCol);
			SelfCategory selfCategory = SelfCategory.valueOf(selfCategoryEnumName);
			pedestrian.setSelfCategory(selfCategory);
		}

		if(groupMembershipCol != ColumnNames.NOT_SET_COLUMN_INDEX_IDENTIFIER) {
			String groupMembershipEnumName = row.getString(groupMembershipCol);
			GroupMembership groupMembership = GroupMembership.valueOf(groupMembershipEnumName);
			pedestrian.setGroupMembership(groupMembership);
		}

		return pedestrian;
	}

	private Row getAgentDataFrameRow(final int pedId) {
		return agentDataFrame.rows(pedId).iterator().next();
	}

	public void setSlice(final double startTime, final double endTime) {
		currentSlice = trajectoryDataFrame/*.where(trajectoryDataFrame.doubleColumn(startTimeCol).isLessThan(endTime)
				.and(trajectoryDataFrame.doubleColumn(endTimeCol).isGreaterThanOrEqualTo(startTime)))*/;
	}

	/**
	 * Returns all footsteps <tt>fs</tt> for which <tt>fs.startTime</tt> is smaller than <tt>endTime</tt>
	 * and <tt>fs.endTime</tt> is greater or equals <tt>startTime</tt>.
	 *
	 * @param startTime
	 * @param endTime
	 * @return multiple foosteps for each agent
	 */
	public Table getAgents(final double startTime, final double endTime) {
		return currentSlice.where(getStartTime().isLessThan(endTime)
				.and(getEndTime().isGreaterThanOrEqualTo(startTime)));
	}

	/**
	 * Returns all footsteps <tt>fs</tt> for which <tt>fs.startTime</tt> is smaller than <tt>endTime</tt>
	 * and <tt>fs.endTime</tt> is greater or equals <tt>startTime</tt> for all agents which are alive in between,
	 * that is: their fist step starts after <tt>startTime</tt> and their last step ends before <tt>endTime</tt>.
	 *
	 * @param startTime
	 * @param endTime
	 * @return multiple foosteps for each agent
	 */
	public Table getAliveAgents(final double startTime, final double endTime) {
		Integer[] filteredPedIds = filterAgents(startTime, endTime);
		return currentSlice.where(getPedId().isIn(filteredPedIds)
				.and(getStartTime().isLessThan(endTime)
						.and(getEndTime().isGreaterThanOrEqualTo(startTime))));
	}

	/**
	 * Returns for all agent at most one footstep which was processed at <tt>simTimeInSec</tt>.
	 *
	 * @param simTimeInSec
	 * @return for all agent at most one footstep
	 */
	public Table getAgents(final double simTimeInSec) {
		return currentSlice.where(
				getStartTime().isLessThanOrEqualTo(simTimeInSec)
						.and(getEndTime().isGreaterThanOrEqualTo(simTimeInSec)))
				.sortAscendingOn(getColumnName(pedIdCol));
	}

	public Table getAgentsWithDisappearedAgents(final double simTimeInSec) {
		Table aliveAgents = getAgents(simTimeInSec);
		Table deadAgents = currentSlice.where(getPedId().isNotIn(getPedId(aliveAgents).asObjectArray())
						.and(getStartTime().isLessThanOrEqualTo(simTimeInSec)));

		Int2IntMap idToRowNr = new Int2IntAVLTreeMap();

		// get the last rows only, this might be computational expensive!
		for(Row row : deadAgents) {
			idToRowNr.put(row.getInt(pedIdCol), row.getRowNumber());
		}

		idToRowNr.forEach((pedId, rowNr) -> Rows.appendRowToTable(rowNr, deadAgents, aliveAgents));

		return aliveAgents;
	}

	/**
	 * Returns at most one footstep (processed at <tt>simTimeInSec</tt>.) for one specific agent
	 * identified by <tt>pedId</tt>.
	 *
	 * @param simTimeInSec  the time
	 * @param pedId         the agent's identifier
	 * @return at most one footstep
	 */
	public Table getAgent(final double simTimeInSec, final int pedId) {
		return currentSlice.where(
				getStartTime().isLessThanOrEqualTo(simTimeInSec)
						.and(getEndTime().isGreaterThanOrEqualTo(simTimeInSec))
						.and(getPedId().isEqualTo(pedId)));
	}

	private Integer[] filterAgents(final double startTime, final double endTime) {
		return agentDataFrame
				.where(getBirthTime().isGreaterThanOrEqualTo(startTime).and(getDeathTime().isGreaterThanOrEqualTo(endTime)))
				.intColumn(agentDFPedIdCol)
				.asObjectArray();
	}

	public Table getCurrentSlice() {
		return currentSlice;
	}

	public boolean isEmpty() {
		return trajectoryDataFrame.isEmpty();
	}

	public double getMaxEndTime() {
		return endTime;
	}

	public double getMinStartTime() {
		return startTime;
	}

	private Table generateAgentDataFrame() {
		if(!isEmpty()) {
			Table agentDataFrame = trajectoryDataFrame
					.summarize(getStartTime(), getEndTime(), min, max).by(getColumnName(pedIdCol));
			agentDataFrame.column(1).setName(birthTimeColName);
			agentDataFrame.column(4).setName(deathTimeColName);
			agentDataFrame.removeColumns(2, 3);
			return agentDataFrame.sortAscendingOn(getColumnName(pedIdCol));
		} else {
			return Table.create();
		}
	}

	private String getColumnName(final int colIndex) {
		return trajectoryDataFrame.columnNames().get(colIndex);
	}

	/**
	 * Searches for the correct column indices.
	 */
	private void searchCols() {

	}

	private IntColumn getPedId(@NotNull final Table table) {
		return table.intColumn(pedIdCol);
	}

	private DoubleColumn getStartX(@NotNull final Table table) {
		return table.doubleColumn(startXCol);
	}

	private DoubleColumn getStartY(@NotNull final Table table) {
		return table.doubleColumn(startYCol);
	}

	private DoubleColumn getEndX(@NotNull final Table table) {
		return table.doubleColumn(endXCol);
	}

	private DoubleColumn getEndY(@NotNull final Table table) {
		return table.doubleColumn(endYCol);
	}

	private DoubleColumn getStartTime(@NotNull final Table table) {
		return table.doubleColumn(startTimeCol);
	}

	private DoubleColumn getEndTime(@NotNull final Table table) {
		return table.doubleColumn(endTimeCol);
	}

	public DoubleColumn getBirthTime() {
		return agentDataFrame.doubleColumn(birthTimeCol);
	}

	public DoubleColumn getDeathTime() {
		return agentDataFrame.doubleColumn(deathTimeCol);
	}

	public double getBirthTime(final int pedId) {
		return agentDataFrame.where(agentDataFrame.intColumn(pedIdCol).isEqualTo(pedId)).doubleColumn(birthTimeCol).get(0);
	}

	public double getDeathTime(final int pedId) {
		double deathTime = agentDataFrame.where(agentDataFrame.intColumn(pedIdCol).isEqualTo(pedId)).doubleColumn(deathTimeCol).get(0);
		return deathTime;
	}

	public IntColumn getPedId() {
		return getPedId(currentSlice);
	}

	public DoubleColumn getStartX() {
		return getStartX(currentSlice);
	}

	public DoubleColumn getStartY() {
		return getStartY(currentSlice);
	}

	public DoubleColumn getEndX() {
		return getEndX(currentSlice);
	}

	public DoubleColumn getEndY() {
		return getEndY(currentSlice);
	}

	public DoubleColumn getStartTime() {
		return getStartTime(currentSlice);
	}

	public DoubleColumn getEndTime() {
		return getEndTime(currentSlice);
	}

	public boolean isValid() {
		return startTimeCol != -1 && endTimeCol == -1;
	}
}
