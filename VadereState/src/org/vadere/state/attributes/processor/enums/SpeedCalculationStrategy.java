package org.vadere.state.attributes.processor.enums;

/**
 *  Offer different strategies to calculate the speed within a measurement area:
 *  - ByTrajectory: Use trajectory.length() / trajectory.duration()
 *  - ByMeasurementAreaHeight: Use measurementArea.height() / trajectory.duration()
 *  - ByMeasurementAreaWidth: Use measurementArea.width() / trajectory.duration()
 */
public enum SpeedCalculationStrategy {
        BY_TRAJECTORY,
        BY_MEASUREMENT_AREA_HEIGHT,
        BY_MEASUREMENT_AREA_WIDTH
}
