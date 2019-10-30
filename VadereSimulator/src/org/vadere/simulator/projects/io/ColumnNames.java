package org.vadere.simulator.projects.io;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import tech.tablesaw.api.Table;

public final class ColumnNames {
	private Set<String> pedestrianIdKeys;
	private Set<String> startX;
	private Set<String> startY;
	private Set<String> endX;
	private Set<String> endY;
	private Set<String> targetIdKeys;
	private Set<String> groupIdKeys;
	private Set<String> groupSizeKeys;
	private Set<String> startTimeKeys;
	private Set<String> endTimeKeys;
	private Set<String> mostImportantStimulusKeys;
	private Set<String> salientBehaviorKeys;
	private List<Set<String>> keys;

	public static final int NOT_SET_COLUMN_INDEX_IDENTIFIER = -1;

	private static ColumnNames instance = null;

	public static ColumnNames getInstance() {
		if(instance == null) {
			instance = new ColumnNames();
		}
		return instance;
	}

	private ColumnNames() {
		keys = new ArrayList<>();
		pedestrianIdKeys = new HashSet<>();
		startX = new HashSet<>();
		startY = new HashSet<>();
		endX = new HashSet<>();
		endY = new HashSet<>();
		targetIdKeys = new HashSet<>();
		groupIdKeys = new HashSet<>();
		groupSizeKeys = new HashSet<>();
		mostImportantStimulusKeys = new HashSet<>();
		salientBehaviorKeys = new HashSet<>();
		startTimeKeys = new HashSet<>();
		endTimeKeys = new HashSet<>();

		//should be set via Processor.getHeader
		pedestrianIdKeys.add("id");
		pedestrianIdKeys.add("pedestrianId");

		startTimeKeys.add("simTime");
		startTimeKeys.add("time");
		startTimeKeys.add("startTime");

		endTimeKeys.add("endTime");

		startX.add("startX");
		startY.add("startY");
		endX.add("endX");
		endY.add("endY");
		targetIdKeys.add("targetId");
		groupIdKeys.add("groupId");
		groupSizeKeys.add("groupSize");
		mostImportantStimulusKeys.add("mostImportantStimulus");
		salientBehaviorKeys.add("salientBehavior");

		keys.add(pedestrianIdKeys);
		keys.add(startX);
		keys.add(startY);
		keys.add(endX);
		keys.add(endY);
		keys.add(targetIdKeys);
		keys.add(groupIdKeys);
		keys.add(groupSizeKeys);
		keys.add(mostImportantStimulusKeys);
		keys.add(salientBehaviorKeys);
		keys.add(startTimeKeys);
		keys.add(endTimeKeys);
	}

	public int getSalientBehaviorCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, salientBehaviorKeys);
	}

	public int getMostImportantEventCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, mostImportantStimulusKeys);
	}

	public int getPedestrianIdCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, pedestrianIdKeys);
	}

	public int getStartTimeCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, startTimeKeys);
	}

	public int getEndTimeCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, endTimeKeys);
	}

	public int getStartXCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, startX);
	}

	public int getStartYCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, startY);
	}

	public int getEndXCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, endX);
	}

	public int getEndYCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, endY);
	}

	public int getTargetIdCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, targetIdKeys);
	}

	public int getGroupIdCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, groupIdKeys);
	}

	public int getGroupSizeCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, groupSizeKeys);
	}

	private int getColId(@NotNull final Table dataFrame, @NotNull final Set<String> possibleHeaderNames) {
		List<String> columnNames = dataFrame.columnNames();

		for (int index = 0; index < columnNames.size(); index++) {
			String headerName = columnNames.get(index).split(OutputFile.headerProcSep)[0];
			if (possibleHeaderNames.contains(headerName)) {
				return index;
			}
		}
		return -1;
	}

	public boolean hasDuplicates(@NotNull final Table dataFrame) {
		for(Set<String> possibleHeaders : keys) {
			int count = 0;
			List<String> columnNames = dataFrame.columnNames();
			for (int index = 0; index < columnNames.size(); index++) {
				String headerName = columnNames.get(index).split(OutputFile.headerProcSep)[0];
				if (possibleHeaders.contains(headerName)) {
					count++;
					if(count > 1) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
