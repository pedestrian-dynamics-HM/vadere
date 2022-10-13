package org.vadere.simulator.utils.scenariochecker;

/**
 * This enum collects all reasons for {@link ScenarioCheckerMessage}s. The label is the messageId
 * for locale de/en. This enum is needed, because the messages are part or the GUI module and a
 * dependency on this module would create a cyclic dependency. A possible solution would be a
 * separated module only for locale messages.
 */
public enum ScenarioCheckerReason {

	// Topography reasons
	SOURCE_TARGET_ID_NOT_FOUND("ScenarioChecker.source.targetIdNotFound"),
	SOURCE_NO_TARGET_ID_SET("ScenarioChecker.source.noTargetIdSet"),
	SOURCE_NO_TARGET_ID_NO_SPAWN("ScenarioChecker.source.noTargetIdAndNoSpawn"),
	SOURCE_ID_NOT_UNIQUE("ScenarioChecker.source.idNotUnique"),
	SOURCE_SPAWN_RND_POS_NOT_FREE_SPACE("ScenarioChecker.source.spawnAtRandomButNotAtFreeSpace"),
	SOURCE_TO_SMALL("ScenarioChecker.source.toSmall"),
	SOURCE_SPAWN_USE_NOT_FREE_SPACE("ScenarioChecker.source.spawnUseNotAtFreeSpace"),

	SOURCE_NEEDS_SPAWNER("ScenarioChecker.source.needsSpawner"),

	STAIRS_TREAD_DIM_WRONG("ScenarioChecker.stairs.wrongTreadDim"),
	TOPOGRAPHY_OFFSET("ScenarioChecker.topography.offset"),

	OBSTACLE_NO_AREA("ScenarioChecker.obstacle.noArea"),
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

	NARROW_BOTTLENECK("ScenarioChecker.obstacle.enclose"),

	TARGET_UNUSED("ScenarioChecker.target.unused"),
	TARGET_OVERLAP("ScenarioChecker.target.overlap"),
	TARGET_NO_ABSORBER("ScenarioChecker.target.noAbsorber"),
	TARGET_NOT_LAST_ABSORBER("ScenarioChecker.target.notLastAbsorber"),
	PEDESTRIAN_SPEED_SETUP("ScenarioChecker.pedestrian.speedsetup"),
	PEDESTRIAN_SPEED_NOT_LOGICAL("ScenarioChecker.pedestrian.speedNotLogical"),
	PEDESTRIAN_SPEED_NEGATIVE("ScenarioChecker.pedestrian.speedIsNegative"),

	// Simulation attribute reasons
	SIM_TIME_STEP_WRONG("ScenarioChecker.simAttr.simTimeStepWrong"),

	PROCESSOR_MEASUREMENT_AREA("ScenarioChecker.processor.measurementarea.doesNotExist"),

	// AreaDensityVoronoiProcessor
	AREAS_DENSITY_VORONOI_PROCESSOR_MISMATCH("ScenarioChecker.processor.checkAreasInAreaDensityVoronoiProcessor.mismatch"),
	MEASUREMENT_AREA_NOT_RECTANGULAR("ScenarioChecker.processor.measurementArea.hasToBeRectangular"),

	// Missing dataprocessors which are necessary in the strategyModel
	DATAPROCESSOR_MISSING("ScenarioChecker.processor.strategyModelDataProcessorCheck."),


	// Group attributes
	GROUP_SETUP_IGNORED("ScenarioChecker.simAttr.GroupSetup.ignored"),

	// Model reasons
	CA_SPAWNING("ScenarioChecker.models.ca.spawning.setup.err"),

	// Psychology layer
	SOCIAL_DISTANCING("ScenarioChecker.models.psychology.socialDistanceValueOutofRange");



	private final String msgId;

	ScenarioCheckerReason(String msgId) {
		this.msgId = msgId;
	}

	public String getLocalMessageId() {
		return msgId;
	}

}
