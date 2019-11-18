package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.attributes.processor.AttributesSpeedInAreaProcessorUsingAgentVelocity;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;

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
 * Watch out: This processor uses the {@link PedestrianVelocityDefaultProcessor} internally instead of the
 * {@link PedestrianTrajectoryProcessor} which is used by {@link PedestrianSpeedInAreaProcessorUsingAgentTrajectory}.
 */
@DataProcessorClass()
public class PedestrianSpeedInAreaProcessorUsingAgentVelocity extends DataProcessor<TimestepPedestrianIdKey, Double> {

	// Static variables
	public static double ERROR_PED_NOT_IN_MEASUREMENT_AREA = -1;

	// Variables
	private MeasurementArea measurementArea;
	private PedestrianVelocityDefaultProcessor pedestrianVelocityDefaultProcessor;

	// Constructors
	public PedestrianSpeedInAreaProcessorUsingAgentVelocity() {
		super("speedInAreaUsingAgentVelocity");
		// "init()" method is used by processor manager to initialize variables.
	}

	// Getter
	public MeasurementArea getMeasurementArea() {
		return this.measurementArea;
	}
	public PedestrianVelocityDefaultProcessor getPedestrianVelocityDefaultProcessor() { return pedestrianVelocityDefaultProcessor; }

	// Methods (overridden)
	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);

		AttributesSpeedInAreaProcessorUsingAgentVelocity processorAttributes = (AttributesSpeedInAreaProcessorUsingAgentVelocity) this.getAttributes();

		// manager.getMeasurementArea() throws an exception if area is "null" or not rectangular. Though, no checks required here.
		boolean rectangularAreaRequired = true;
		measurementArea = manager.getMeasurementArea(processorAttributes.getMeasurementAreaId(), rectangularAreaRequired);

		pedestrianVelocityDefaultProcessor = (PedestrianVelocityDefaultProcessor) manager.getProcessor(processorAttributes.getPedestrianVelocityDefaultProcessorId());

		if (pedestrianVelocityDefaultProcessor == null) {
			throw new RuntimeException(String.format("PedestrianVelocityDefaultProcessor with index %d does not exist.", processorAttributes.getPedestrianVelocityDefaultProcessorId()));
		}
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesSpeedInAreaProcessorUsingAgentVelocity());
		}

		return super.getAttributes();
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		pedestrianVelocityDefaultProcessor.update(state);

		for (Pedestrian pedestrian : state.getTopography().getElements(Pedestrian.class)) {
			double speed = ERROR_PED_NOT_IN_MEASUREMENT_AREA;
			TimestepPedestrianIdKey rowKey = new TimestepPedestrianIdKey(state.getStep(), pedestrian.getId());

			if (measurementArea.getShape().contains(pedestrian.getPosition())) {
				speed = pedestrianVelocityDefaultProcessor.getValue(rowKey);
			}

			this.putValue(rowKey, speed);
		}
	}

}
