package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesAreaProcessor;
import org.vadere.state.attributes.processor.AttributesPedestrianVelocityProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.attributes.processor.AttributesSpeedInAreaProcessor;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Log for each pedestrian the speed within an measurement area.
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
 *       log 0 speed for P1.
 *
 * <pre>
 * | timeStep | pedId | ... | M1  | M2  |
 * |----------|-------|-----|-----|-----|
 * |1         | 1     |     | 0.5 | 0   |
 * |1         | 2     |     | 0   | 0.6 |
 * </pre>
 */
@DataProcessorClass()
public class PedestrianSpeedInAreaProcessor extends DataProcessor<TimestepPedestrianIdKey, Double> {

	// Variables
	private MeasurementArea measurementArea;
	private PedestrianVelocityProcessor pedestrianVelocityProcessor;

	// Constructors
	public PedestrianSpeedInAreaProcessor() {
		super("speedInArea");

		// "measurementArea" and "pedestrianVelocityProcessor"
		// are initialized in "init()" method.
	}

	// Getter
	public MeasurementArea getMeasurementArea() {
		return this.measurementArea;
	}
	public PedestrianVelocityProcessor getPedestrianVelocityProcessor() { return pedestrianVelocityProcessor; }

	// Methods (overridden)
	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);

		AttributesSpeedInAreaProcessor processorAttributes = (AttributesSpeedInAreaProcessor) this.getAttributes();

		measurementArea = manager.getMeasurementArea(processorAttributes.getMeasurementAreaId());
		pedestrianVelocityProcessor = (PedestrianVelocityProcessor) manager.getProcessor(processorAttributes.getPedestrianVelocityProcessorId());

		if (measurementArea == null)
			throw new RuntimeException(String.format("MeasurementArea with index %d does not exist.", processorAttributes.getMeasurementAreaId()));
		if (pedestrianVelocityProcessor == null)
			throw new RuntimeException(String.format("PedestrianVelocityProcessor with index %d does not exist.", processorAttributes.getPedestrianVelocityProcessorId()));
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
		// TODO: "pedestrianVelocityProcessor.doUpdate()" is called by top-level process manager automatically.
		//   Clarify with Bene if correct update order is always ensured!

		for (Pedestrian pedestrian : state.getTopography().getElements(Pedestrian.class)) {
			TimestepPedestrianIdKey rowKey = new TimestepPedestrianIdKey(state.getStep(), pedestrian.getId());
			double speed = pedestrianVelocityProcessor.getValue(rowKey);

			if (measurementArea.getShape().contains(pedestrian.getPosition()) == false) {
				speed = 0;
			}

			this.putValue(rowKey, speed);
		}
	}

}
