package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.attributes.processor.AttributesSpeedInAreaProcessor;
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
 * |1         | 1     |     | 0.5 | 0   |
 * |1         | 2     |     | 0   | 0.6 |
 * </pre>
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
public class PedestrianSpeedInAreaProcessor extends DataProcessor<TimestepPedestrianIdKey, Double> {

	// Variables
	private MeasurementArea measurementArea;
	private PedestrianTrajectoryProcessor pedestrianTrajectoryProcessor;
	private BiFunction<VTrajectory, VRectangle, Double> speedCalculationStrategy;

	// Constructors
	public PedestrianSpeedInAreaProcessor() {
		super("speedInArea");
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

		AttributesSpeedInAreaProcessor processorAttributes = (AttributesSpeedInAreaProcessor) this.getAttributes();

		measurementArea = manager.getMeasurementArea(processorAttributes.getMeasurementAreaId());
		pedestrianTrajectoryProcessor = (PedestrianTrajectoryProcessor) manager.getProcessor(processorAttributes.getPedestrianTrajectoryProcessorId());

		if (measurementArea == null) {
			throw new RuntimeException(String.format("MeasurementArea with index %d does not exist.", processorAttributes.getMeasurementAreaId()));
		}
		if (measurementArea.getShape() instanceof VRectangle == false) {
			throw new RuntimeException("MeasurementArea should be rectangular.");
		}
		if (pedestrianTrajectoryProcessor == null) {
			throw new RuntimeException(String.format("PedestrianVelocityProcessor with index %d does not exist.", processorAttributes.getPedestrianTrajectoryProcessorId()));
		}

		initSpeedCalculationStrategy(processorAttributes);
	}

	private void initSpeedCalculationStrategy(AttributesSpeedInAreaProcessor processorAttributes) {
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
			setAttributes(new AttributesSpeedInAreaProcessor());
		}

		return super.getAttributes();
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		// TODO: Clarify with Bene if it ensured, that "pedestrianTrajectoryProcessor.doUpdate()"
		//   is always invoked automatically by underlying processor manager.
		AttributesSpeedInAreaProcessor processorAttributes = (AttributesSpeedInAreaProcessor) this.getAttributes();

		for (Pedestrian pedestrian : state.getTopography().getElements(Pedestrian.class)) {
			double speed = -1;

			if (measurementArea.getShape().contains(pedestrian.getPosition())) {
				// Use pedestrian's trajectory to calculate the speed.
				VTrajectory wholeTrajectory = pedestrianTrajectoryProcessor.getValue(new PedestrianIdKey(pedestrian.getId()));
				VTrajectory cuttedTrajectory = wholeTrajectory.cut(measurementArea.asVRectangle());

				speed = speedCalculationStrategy.apply(cuttedTrajectory, measurementArea.asVRectangle());
			}

			TimestepPedestrianIdKey rowKey = new TimestepPedestrianIdKey(state.getStep(), pedestrian.getId());
			this.putValue(rowKey, speed);
		}
	}

	private double calculateSpeedByTrajectory(VTrajectory trajectory, VRectangle measurementArea) {
		double speed = Double.NaN;

		if (trajectory.speed().isPresent()) {
			speed = trajectory.speed().get();
		}

		return speed;
	}

	private double calculateSpeedByMeasurementAreaHeight(VTrajectory trajectory, VRectangle measurementArea) {
		double speed = (measurementArea.height / trajectory.duration());
		return speed;
	}

	private double calculateSpeedByMeasurementAreaWidth(VTrajectory trajectory, VRectangle measurementArea) {
		double speed = measurementArea.width / trajectory.duration();
		return speed;
	}

}
