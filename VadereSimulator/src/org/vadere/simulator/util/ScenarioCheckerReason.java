package org.vadere.simulator.util;

/**
 * This enum collects all reasons for {@link ScenarioCheckerMessage}s. The label is the messageId
 * for locale de/en. This enum is needed, because the messages are part or the GUI module and a
 * dependency on this module would create a cyclic dependency. A possible solution would be a
 * separated module only for locale messages.
 */
public enum ScenarioCheckerReason {

	SOURCE_TARGET_ID_NOT_FOUND("ScenarioChecker.source.targetIdNotFound"),
	SOURCE_NO_TARGET_ID_SET("ScenarioChecker.source.noTargetIdSet"),
	SOURCE_NO_TARGET_ID_NO_SPAWN("ScenarioChecker.source.noTargetIdAndNoSpawn"),
	SOURCE_ID_NOT_UNIQUE("ScenarioChecker.source.idNotUnique"),
	STAIRS_TREAD_DIM_WRONG("ScenarioChecker.stairs.wrongTreadDim"),

	OVERLAP_STAIR_STAIR("ScenarioChecker.overlap.stair.stair"),
	OVERLAP_TARGET_TARGET("ScenarioChecker.overlap.target.target"),
	OVERLAP_TARGET_STAIR("ScenarioChecker.overlap.target.stair"),
	OVERLAP_SOURCE_STAIR("ScenarioChecker.overlap.source.stair"),
	OVERLAP_SOURCE_TARGET("ScenarioChecker.overlap.source.target"),
	OVERLAP_SOURCE_SOURCE("ScenarioChecker.overlap.source.source"),
	OVERLAP_OBSTACLE_STAIRS_ERR("ScenarioChecker.overlap.obstacle.stairs.err"),
	OVERLAP_OBSTACLE_STAIRS_WARN("ScenarioChecker.overlap.obstacle.stairs.warn"),
	OVERLAP_OBSTACLE_TARGET_ERR("ScenarioChecker.overlap.obstacle.target.err"),
	OVERLAP_OBSTACLE_TARGET_WARN("ScenarioChecker.overlap.obstacle.target.warn"),
	OVERLAP_OBSTACLE_SOURCE("ScenarioChecker.overlap.obstacle.source"),
	OVERLAP_OBSTACLE_OBSTACLE("ScenarioChecker.overlap.obstacle.obstacle"),
	OVERLAP_OBSTACLE_OBSTACLE_ENCLOSE("ScenarioChecker.overlap.obstacle.obstacle.enclose"),

	TARGET_UNUSED("ScenarioChecker.target.unused"),
	TARGET_OVERLAP("ScenarioChecker.target.overlap"),
	PEDESTRIAN_SPEED_SETUP("ScenarioChecker.pedestrian.speedsetup"),
	PEDESTRIAN_SPEED_NOT_LOGICAL("ScenarioChecker.pedestrian.speedNotLogical"),
	PEDESTRIAN_SPEED_NEGATIVE("ScenarioChecker.pedestrian.speedIsNegative");

	private String msgId;

	ScenarioCheckerReason(String msgId) {
		this.msgId = msgId;
	}

	public String getLocalMessageId() {
		return msgId;
	}

}
