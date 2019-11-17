package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.attributes.processor.AttributesSpeedInAreaProcessorUsingAgentTrajectory;
import org.vadere.state.attributes.processor.enums.SpeedCalculationStrategy;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.function.BiFunction;

/**
 * Log for each pedestrian the speed within a measurement area.
 *
 * <pre>
 * +--------------------------------------------------+
 * |                                                  |
 * |        M1-------------+  M2-------------+        |
 * | +-+    |              |  |              |    +-+ |
 * | |S|    |      P1      |  |      P2      |    |T| |
 * | +-+    |              |  |              |    +-+ |
 * |        +--------------+  +--------------+        |
 * |                                                  |
 * +--------------------------------------------------+
 *
 * - S: source
 * - T: target
 * - Pi: pedestrian i
 * - Mj: measurement j
 * </pre>
 *
 * Note: If two measurement areas M1 and M2 are disjoint
 *       and a pedestrian P1 is located within M1, M2 should
 *       log a speed of -1 for P1.
 *
 * <pre>
 * | timeStep | pedId | ... | M1  | M2  |
 * |----------|-------|-----|-----|-----|
 * |1         | 1     |     | 0.5 | -1  |
 * |1         | 2     |     | -1  | 0.6 |
 * </pre>
 *
 * Note: If trajectory of pedestrian is empty, log -2.
 *
 * Use the {@link PedestrianTrajectoryProcessor} to access pedestrian's
 * trajectory.
 *
 * This processor offers different methods do calculate pedestrian's speed:
 * - ByTrajectory: Use {@link VTrajectory#speed()}, i.e. trajectory.length() / trajectory.duration()
 * - ByMeasurementAreaHeight: Use measurementArea.height() / trajectory.duration()
 * - ByMeasurementAreaWidth: Use measurementArea.width() / trajectory.duration()
 */
@DataProcessorClass()
public class PedestrianSpeedInAreaProcessorUsingAgentTrajectory extends DataProcessor<TimestepPedestrianIdKey, Double> {

	// Static variables
	public static double ERROR_PED_NOT_IN_MEASUREMENT_AREA = -1;
	public static double ERROR_NO_TRAJECTORY_AVAILABLE = -2;

	// Variables
	private MeasurementArea measurementArea;
	private PedestrianTrajectoryProcessor pedestrianTrajectoryProcessor;
	private BiFunction<VTrajectory, VRectangle, Double> speedCalculationStrategy;

	// Constructors
	public PedestrianSpeedInAreaProcessorUsingAgentTrajectory() {
		super("speedInAreaUsingAgentTrajectory");
		// "init()" method is used by processor manager to initialize variables.
	}

	// Getter
	public MeasurementArea getMeasurementArea() {
		return this.measurementArea;
	}
	public PedestrianTrajectoryProcessor getPedestrianTrajectoryProcessor() { return pedestrianTrajectoryProcessor; }

	// Methods (overridden)
	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);

		AttributesSpeedInAreaProcessorUsingAgentTrajectory processorAttributes = (AttributesSpeedInAreaProcessorUsingAgentTrajectory) this.getAttributes();

		// manager.getMeasurementArea() throws an exception if area is "null" or not rectangular. Though, no checks required here.
		boolean rectangularAreaRequired = true;
		measurementArea = manager.getMeasurementArea(processorAttributes.getMeasurementAreaId(), rectangularAreaRequired);

		pedestrianTrajectoryProcessor = (PedestrianTrajectoryProcessor) manager.getProcessor(processorAttributes.getPedestrianTrajectoryProcessorId());

		if (pedestrianTrajectoryProcessor == null) {
			throw new RuntimeException(String.format("PedestrianTrajectoryProcessor with index %d does not exist.", processorAttributes.getPedestrianTrajectoryProcessorId()));
		}

		initSpeedCalculationStrategy(processorAttributes);
	}

	private void initSpeedCalculationStrategy(AttributesSpeedInAreaProcessorUsingAgentTrajectory processorAttributes) {
		if (processorAttributes.getSpeedCalculationStrategy() == SpeedCalculationStrategy.BY_TRAJECTORY) {
			speedCalculationStrategy = this::calculateSpeedByTrajectory;
		} else if (processorAttributes.getSpeedCalculationStrategy() == SpeedCalculationStrategy.BY_MEASUREMENT_AREA_HEIGHT) {
			speedCalculationStrategy = this::calculateSpeedByMeasurementAreaHeight;
		} else if (processorAttributes.getSpeedCalculationStrategy() == SpeedCalculationStrategy.BY_MEASUREMENT_AREA_WIDTH) {
			speedCalculationStrategy = this::calculateSpeedByMeasurementAreaWidth;
		} else {
			throw new RuntimeException("Unsupported speedCalculationStrategy.");
		}
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesSpeedInAreaProcessorUsingAgentTrajectory());
		}

		return super.getAttributes();
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		pedestrianTrajectoryProcessor.update(state);

		for (Pedestrian pedestrian : state.getTopography().getElements(Pedestrian.class)) {
			double speed = ERROR_PED_NOT_IN_MEASUREMENT_AREA;

			if (measurementArea.getShape().contains(pedestrian.getPosition())) {
				VTrajectory wholeTrajectory = pedestrianTrajectoryProcessor.getValue(new PedestrianIdKey(pedestrian.getId()));
				VTrajectory cuttedTrajectory = wholeTrajectory.cut(measurementArea.asVRectangle());

				speed = speedCalculationStrategy.apply(cuttedTrajectory, measurementArea.asVRectangle());
			}

			TimestepPedestrianIdKey rowKey = new TimestepPedestrianIdKey(state.getStep(), pedestrian.getId());
			this.putValue(rowKey, speed);
		}
	}

	private double calculateSpeedByTrajectory(VTrajectory trajectory, VRectangle measurementArea) {
		double speed = ERROR_NO_TRAJECTORY_AVAILABLE;

		if (trajectory.speed().isPresent()) {
			speed = trajectory.speed().get();
		}

		return speed;
	}

	private double calculateSpeedByMeasurementAreaHeight(VTrajectory trajectory, VRectangle measurementArea) {
		double speed = ERROR_NO_TRAJECTORY_AVAILABLE;

		if (trajectory.duration().isPresent()) {
			speed = (measurementArea.height / trajectory.duration().get());
		}

		return speed;
	}

	private double calculateSpeedByMeasurementAreaWidth(VTrajectory trajectory, VRectangle measurementArea) {
		double speed = ERROR_NO_TRAJECTORY_AVAILABLE;

		if (trajectory.duration().isPresent()) {
			speed = measurementArea.width / trajectory.duration().get();
		}

		return speed;
	}

}
