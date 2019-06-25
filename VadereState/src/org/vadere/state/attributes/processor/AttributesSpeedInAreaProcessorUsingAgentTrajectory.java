package org.vadere.state.attributes.processor;

import org.vadere.state.attributes.processor.enums.SpeedCalculationStrategy;

public class AttributesSpeedInAreaProcessorUsingAgentTrajectory extends AttributesProcessor {

    // Variables
	private int measurementAreaId = -1;
    private int pedestrianTrajectoryProcessorId = -1;
    private SpeedCalculationStrategy speedCalculationStrategy = SpeedCalculationStrategy.BY_TRAJECTORY;

    // Getter
    public int getMeasurementAreaId() {
        return this.measurementAreaId;
    }
    public int getPedestrianTrajectoryProcessorId() {
        return this.pedestrianTrajectoryProcessorId;
    }
    public SpeedCalculationStrategy getSpeedCalculationStrategy() { return speedCalculationStrategy; }

    // Setter
    public void setMeasurementAreaId(int measurementAreaId) {
        checkSealed();
        this.measurementAreaId = measurementAreaId;
    }

    public void setPedestrianTrajectoryProcessorId(int pedestrianTrajectoryProcessorId) {
        checkSealed();
        this.pedestrianTrajectoryProcessorId = pedestrianTrajectoryProcessorId;
    }

    public void setSpeedCalculationStrategy(SpeedCalculationStrategy speedCalculationStrategy) {
        this.speedCalculationStrategy = speedCalculationStrategy;
    }

}
