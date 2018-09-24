package org.vadere.simulator.util;

/**
 * This enum collects all reasons for {@link TopographyCheckerMessage}s. The label is the messageId
 * for locale de/en. This enum is needed, because the messages are part or the GUI module and a
 * dependency on this module would create a cyclic dependency. A possible solution would be a
 * separated module only for locale messages.
 */
public enum TopographyCheckerReason {

	SOURCE_TARGET_ID_NOT_FOUND("TopographyChecker.source.targetIdNotFound"),
	SOURCE_NO_TARGET_ID_SET("TopographyChecker.source.noTargetIdSet"),
	SOURCE_NO_TARGET_ID_NO_SPAWN("TopographyChecker.source.noTargetIdAndNoSpawn"),
	SOURCE_ID_NOT_UNIQUE("TopographyChecker.source.idNotUnique"),
	STAIRS_TREAD_DIM_WRONG("TopographyChecker.stairs.wrongTreadDim"),

	OVERLAP_STAIR_STAIR("TopographyChecker.overlap.stair.stair"),
	OVERLAP_TARGET_TARGET("TopographyChecker.overlap.target.target"),
	OVERLAP_TARGET_STAIR("TopographyChecker.overlap.target.stair"),
	OVERLAP_SOURCE_STAIR("TopographyChecker.overlap.source.stair"),
	OVERLAP_SOURCE_TARGET("TopographyChecker.overlap.source.target"),
	OVERLAP_SOURCE_SOURCE("TopographyChecker.overlap.source.source"),
	OVERLAP_OBSTACLE_STAIRS_ERR("TopographyChecker.overlap.obstacle.stairs.err"),
	OVERLAP_OBSTACLE_STAIRS_WARN("TopographyChecker.overlap.obstacle.stairs.warn"),
	OVERLAP_OBSTACLE_TARGET_ERR("TopographyChecker.overlap.obstacle.target.err"),
	OVERLAP_OBSTACLE_TARGET_WARN("TopographyChecker.overlap.obstacle.target.warn"),
	OVERLAP_OBSTACLE_SOURCE("TopographyChecker.overlap.obstacle.source"),
	OVERLAP_OBSTACLE_OBSTACLE("TopographyChecker.overlap.obstacle.obstacle"),
	OVERLAP_OBSTACLE_OBSTACLE_ENCLOSE("TopographyChecker.overlap.obstacle.obstacle.enclose"),

	TARGET_UNUSED("TopographyChecker.target.unused"),
	TARGET_OVERLAP("TopographyChecker.target.overlap"),
	PEDESTRIAN_SPEED_SETUP("TopographyChecker.pedestrian.speedsetup"),
	PEDESTRIAN_SPEED_NOT_LOGICAL("TopographyChecker.pedestrian.speedNotLogical"),
	PEDESTRIAN_SPEED_NEGATIVE("TopographyChecker.pedestrian.speedIsNegative");

	private String msgId;

	TopographyCheckerReason(String msgId) {
		this.msgId = msgId;
	}

	public String getLocalMessageId() {
		return msgId;
	}

}
