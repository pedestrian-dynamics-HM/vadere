package org.vadere.gui.postvisualization.model;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.io.ColumnNames;
import tech.tablesaw.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link ContactData}
 */
public class ContactData {

	private final Table contactsDataFrame;
	private final List<Integer> rowsWithPedIds;
	private static final double SIM_STEP_LENGTH = 0.4;

	public static final String TABLE_NAME = "contacts";

	// columns, TODO: this is hard coded!
	public final int startTimeStepCol;
	public final int firstPedIdCol;
	public final int secondPedIdCol;
	public final int durationCol;
	public final int xPathCol;
	public final int yPathCol;

	/**
	 * Default constructor.
	 *
	 * @param dataFrame the whole table containing all trajectories of all contacts for all times
	 */
	public ContactData(@NotNull final Table dataFrame) {
		// get all ids of all columns
		// 1. mandatory columns:
		ColumnNames columnNames = ColumnNames.getInstance();
		startTimeStepCol = columnNames.getStartTimeCol(dataFrame);
		firstPedIdCol = columnNames.getPedestrianIdCol(dataFrame);
		secondPedIdCol = columnNames.getSecondPedestrianIdCol(dataFrame);
		durationCol = columnNames.getDurationCol(dataFrame);
		xPathCol = columnNames.getStartXCol(dataFrame);
		yPathCol = columnNames.getStartYCol(dataFrame);

		this.contactsDataFrame = dataFrame;
		rowsWithPedIds = new ArrayList<>();
		if (!isEmpty()) {
			StringColumn startTimes = getStartTimeStep(contactsDataFrame);
			for (int i = 0; i < startTimes.size(); i++) {
				if (!startTimes.get(i).equals("-")) {
					rowsWithPedIds.add(i);
				}
			}
		}
	}

	public boolean isEmpty() {
		return contactsDataFrame.isEmpty();
	}

	public List<Integer> getRowsWithPedIds() {
		return rowsWithPedIds;
	}

	private StringColumn getStartTimeStep(@NotNull final Table table) {
		return table.stringColumn(startTimeStepCol);
	}

	private StringColumn getFirstPedIdCol(@NotNull final Table table) {
		return table.stringColumn(firstPedIdCol);
	}

	private StringColumn getSecondPedIdCol(@NotNull final Table table) {
		return table.stringColumn(secondPedIdCol);
	}

	private StringColumn getDurationCol(@NotNull final Table table) {
		return table.stringColumn(durationCol);
	}

	private DoubleColumn getXPathCol(@NotNull final Table table) {
		return table.doubleColumn(xPathCol);
	}

	private DoubleColumn getYPathCol(@NotNull final Table table) {
		return table.doubleColumn(yPathCol);
	}

	public List<Table> getTrajectoriesOfContactsUntil(double simTime) {
		List<Table> result = new ArrayList<>();
		for (Integer rowIndex: rowsWithPedIds) {
			double contactStartTime = Double.parseDouble(getStartTimeStep(contactsDataFrame).get(rowIndex)) * SIM_STEP_LENGTH;
			int durationTimeSteps = Integer.parseInt(getDurationCol(contactsDataFrame).get(rowIndex));
			double contactDurationTime = durationTimeSteps * SIM_STEP_LENGTH;
			boolean contactStartedBeforeSimTime = contactStartTime < simTime;
			if (contactStartedBeforeSimTime) {
				boolean contactHasNotEndedAtSimTime = simTime < contactStartTime + contactDurationTime;
				int currentTimeStep = (int)Math.round(simTime);
				int diffContactStartToCurrentTimestep = currentTimeStep - (int)contactStartTime;
				if (diffContactStartToCurrentTimestep == 0) {
					diffContactStartToCurrentTimestep = 1;
				}
				try {
					DoubleColumn xCol = getXPathCol(contactsDataFrame).inRange(rowIndex, Math.min(rowIndex + durationTimeSteps, rowIndex + diffContactStartToCurrentTimestep));
					DoubleColumn yCol = getYPathCol(contactsDataFrame).inRange(rowIndex, Math.min(rowIndex + durationTimeSteps, rowIndex + diffContactStartToCurrentTimestep));
					Table trajectoryOfSingleContact = Table.create(xCol, yCol);
					result.add(trajectoryOfSingleContact);
				} catch (Exception e) {
					System.out.println("yeet");
				}

			}
		}
		return result;
	}
	public Table getPairsOfPedestriansInContactAt(double simTime) {
		IntColumn ped1IDs = IntColumn.create("ped1Ids");
		IntColumn ped2IDs = IntColumn.create("ped2Ids");
		for (Integer rowIndex: rowsWithPedIds) {
			double contactStartTime = Double.parseDouble(getStartTimeStep(contactsDataFrame).get(rowIndex)) * SIM_STEP_LENGTH;
			int durationTimeSteps = Integer.parseInt(getDurationCol(contactsDataFrame).get(rowIndex));
			double contactDurationTime = durationTimeSteps * SIM_STEP_LENGTH;
			boolean contactAtSimTime = contactStartTime < simTime && simTime < contactStartTime + contactDurationTime;
			if (contactAtSimTime) {
				int firstPedId = Integer.parseInt(getFirstPedIdCol(contactsDataFrame).get(rowIndex));
				int secondPedId = Integer.parseInt(getSecondPedIdCol(contactsDataFrame).get(rowIndex));
				ped1IDs.append(firstPedId);
				ped2IDs.append(secondPedId);
			}
		}
		return Table.create(ped1IDs, ped2IDs);
	}
}
