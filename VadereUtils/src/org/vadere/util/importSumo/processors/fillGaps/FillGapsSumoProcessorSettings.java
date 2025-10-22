package org.vadere.util.importSumo.processors.fillGaps;

public class FillGapsSumoProcessorSettings {
    private final boolean enableLaneToJunctionSnapping;
    private final double laneToJunctionMaxSnappingDistance;
    private final boolean enableCrosswalkToEdgeSnapping;
    private final double crosswalkToEdgeSnappingMaxDistance;
    private final double crosswalkToEdgeSnappingMaxAngle;

    public FillGapsSumoProcessorSettings(boolean enableLaneToJunctionSnapping, double laneToJunctionMaxSnappingDistance, boolean enableCrosswalkToEdgeSnapping, double crosswalkToEdgeSnappingMaxDistance, double crosswalkToEdgeSnappingMaxAngle) {
        this.enableLaneToJunctionSnapping = enableLaneToJunctionSnapping;
        this.laneToJunctionMaxSnappingDistance = laneToJunctionMaxSnappingDistance;
        this.enableCrosswalkToEdgeSnapping = enableCrosswalkToEdgeSnapping;
        this.crosswalkToEdgeSnappingMaxDistance = crosswalkToEdgeSnappingMaxDistance;
        this.crosswalkToEdgeSnappingMaxAngle = crosswalkToEdgeSnappingMaxAngle;
    }

    public boolean isEnableLaneToJunctionSnapping() {
        return enableLaneToJunctionSnapping;
    }

    public double getLaneToJunctionMaxSnappingDistance() {
        return laneToJunctionMaxSnappingDistance;
    }

    public double getCrosswalkToEdgeSnappingMaxDistance() {
        return crosswalkToEdgeSnappingMaxDistance;
    }

    public boolean isEnableCrosswalkToWalkwaysSnapping() {
        return enableCrosswalkToEdgeSnapping;
    }

    public double getCrosswalkToEdgeSnappingMaxAngle() {
        return crosswalkToEdgeSnappingMaxAngle;
    }
}
