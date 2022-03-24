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
	private Set<String> secondPedestrianIdKeys;
	private Set<String> startX;
	private Set<String> startY;
	private Set<String> endX;
	private Set<String> endY;
	private Set<String> targetIdKeys;
	private Set<String> groupIdKeys;
	private Set<String> groupSizeKeys;
	private Set<String> startTimeKeys;
	private Set<String> endTimeKeys;
	private Set<String> durationKeys;
	private Set<String> mostImportantStimulusKeys;
	private Set<String> selfCategoryKeys;
	private Set<String> informationStateKeys;
	private Set<String> groupMembershipKeys;
	private Set<String> isInfectiousKeys;
	private Set<String> degreeOfExposureKeys;
	private Set<String> aerosolCloudIdKeys;
	private Set<String> aerosolCloudPathogenLoadKeys;
	private Set<String> aerosolCloudRadiusKeys;
	private Set<String> aerosolCloudCenterXKeys;
	private Set<String> aerosolCloudCenterYKeys;
	private Set<String> timeStepKeys; //ToDo replace timeStepKeys by start/endTimeKeys?

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
		secondPedestrianIdKeys = new HashSet<>();
		startX = new HashSet<>();
		startY = new HashSet<>();
		endX = new HashSet<>();
		endY = new HashSet<>();
		targetIdKeys = new HashSet<>();
		groupIdKeys = new HashSet<>();
		groupSizeKeys = new HashSet<>();
		mostImportantStimulusKeys = new HashSet<>();
		selfCategoryKeys = new HashSet<>();
		informationStateKeys = new HashSet<>();
		groupMembershipKeys = new HashSet<>();
		isInfectiousKeys = new HashSet<>();
		degreeOfExposureKeys = new HashSet<>();
		aerosolCloudIdKeys = new HashSet<>();
		aerosolCloudPathogenLoadKeys = new HashSet<>();
		aerosolCloudRadiusKeys = new HashSet<>();
		aerosolCloudCenterXKeys = new HashSet<>();
		aerosolCloudCenterYKeys = new HashSet<>();
		timeStepKeys = new HashSet<>();
		startTimeKeys = new HashSet<>();
		endTimeKeys = new HashSet<>();
		durationKeys = new HashSet<>();

		//should be set via Processor.getHeader
		pedestrianIdKeys.add("id");
		pedestrianIdKeys.add("pedestrianId");
		pedestrianIdKeys.add("1stPedId");

		secondPedestrianIdKeys.add("2ndPedId");


		startTimeKeys.add("simTime");
		startTimeKeys.add("time");
		startTimeKeys.add("startTime");
		startTimeKeys.add("startTimeStep");


		endTimeKeys.add("endTime");

		durationKeys.add("durationTimesteps");

		startX.add("startX");
		startX.add("xPath");
		startY.add("startY");
		startY.add("yPath");
		endX.add("endX");
		endY.add("endY");
		targetIdKeys.add("targetId");
		groupIdKeys.add("groupId");
		groupSizeKeys.add("groupSize");
		mostImportantStimulusKeys.add("mostImportantStimulus");
		selfCategoryKeys.add("selfCategory");
		informationStateKeys.add("informationState");
		groupMembershipKeys.add("groupMembership");

		isInfectiousKeys.add("isInfectious");
		degreeOfExposureKeys.add("degreeOfExposure");

		aerosolCloudIdKeys.add("id");
		aerosolCloudPathogenLoadKeys.add("pathogenLoad");
		aerosolCloudRadiusKeys.add("radius");
		aerosolCloudCenterXKeys.add("centerX");
		aerosolCloudCenterYKeys.add("centerY");

		timeStepKeys.add("timeStep");

		keys.add(pedestrianIdKeys);
		keys.add(secondPedestrianIdKeys);
		keys.add(startX);
		keys.add(startY);
		keys.add(endX);
		keys.add(endY);
		keys.add(targetIdKeys);
		keys.add(groupIdKeys);
		keys.add(groupSizeKeys);
		keys.add(mostImportantStimulusKeys);
		keys.add(selfCategoryKeys);
		keys.add(informationStateKeys);
		keys.add(groupMembershipKeys);
		keys.add(isInfectiousKeys);
		keys.add(degreeOfExposureKeys);
		keys.add(aerosolCloudIdKeys);
		keys.add(aerosolCloudPathogenLoadKeys);
		keys.add(aerosolCloudRadiusKeys);
		keys.add(aerosolCloudCenterXKeys);
		keys.add(aerosolCloudCenterYKeys);
		keys.add(timeStepKeys);
		keys.add(startTimeKeys);
		keys.add(endTimeKeys);
		keys.add(durationKeys);
	}

	public int getMostImportantStimulusCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, mostImportantStimulusKeys);
	}

	public int getSelfCategoryCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, selfCategoryKeys);
	}

	public int getInformationStateCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, informationStateKeys);
	}

	public int getGroupMembershipCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, groupMembershipKeys);
	}

	public int getIsInfectiousCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, isInfectiousKeys);
	}

	public int getDegreeOfExposureCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, degreeOfExposureKeys);
	}

	public int getAerosolCloudIdCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, aerosolCloudIdKeys);
	}

	public int getAerosolCloudPathogenLoadCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, aerosolCloudPathogenLoadKeys);
	}

	public int getAerosolCloudRadiusCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, aerosolCloudRadiusKeys);
	}

	public int getAerosolCloudCenterXCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, aerosolCloudCenterXKeys);
	}
	public int getAerosolCloudCenterYCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, aerosolCloudCenterYKeys);
	}

	public int getTimeStepCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, timeStepKeys);
	}

	public int getPedestrianIdCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, pedestrianIdKeys);
	}
	public int getSecondPedestrianIdCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, secondPedestrianIdKeys);
	}
	public int getDurationCol(@NotNull final Table dataFrame) {
		return getColId(dataFrame, durationKeys);
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
