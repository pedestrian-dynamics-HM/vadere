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
	SOURCE_OVERLAP_WITH_OBSTACLE("TopographyChecker.source.overlapWithObstacle"),
	STAIRS_TREAD_DIM_WRONG("TopographyChecker.stairs.wrongTreadDim"),
	TARGET_UNUSED("TopographyChecker.target.unused"),
	OBSTACLES_OVERLAP("TopographyChecker.obstacles.overlap"),
	SOURCE_OVERLAP("TopographyChecker.source.overlap"),
	TARGET_OVERLAP("TopographyChecker.target.overlap"),
	STAIRS_OVERLAP("TopographyChecker.stairs.overlap"),
	PEDESTRIAN_SPEED_SETUP("TopographyChecker.pedestrian.speedsetup");

	private String msgId;

	TopographyCheckerReason(String msgId) {
		this.msgId = msgId;
	}

	public String getLocalMessageId() {
		return msgId;
	}

}
