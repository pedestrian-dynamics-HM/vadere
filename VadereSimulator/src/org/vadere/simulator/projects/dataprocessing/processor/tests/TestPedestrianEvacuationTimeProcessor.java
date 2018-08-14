package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesTestNumberOverlapsProcessor;
import org.vadere.state.attributes.processor.AttributesTestPedestrianEvacuationTimeProcessor;

/**
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class TestPedestrianEvacuationTimeProcessor extends TestProcessor {

	private PedestrianEvacuationTimeProcessor pedestrianEvacuationTimeProcessor;

	public TestPedestrianEvacuationTimeProcessor() {
		super("test-pedestrianEvacuationTime");
		setAttributes(new AttributesTestPedestrianEvacuationTimeProcessor());
	}

	@Override
	public void init(@NotNull final ProcessorManager manager) {
		super.init(manager);
		AttributesTestPedestrianEvacuationTimeProcessor att = this.getAttributes();
		pedestrianEvacuationTimeProcessor =
				(PedestrianEvacuationTimeProcessor) manager.getProcessor(att.getPedestrianEvacuationTimeProcessorId());
	}

	@Override
	protected void doUpdate(@NotNull final SimulationState state) {
		pedestrianEvacuationTimeProcessor.update(state);
	}

	@Override
	public void preLoop(SimulationState state) {
		pedestrianEvacuationTimeProcessor.preLoop(state);
	}

	@Override
	public void postLoop(SimulationState state) {
		pedestrianEvacuationTimeProcessor.postLoop(state);

		int invalidEvacuationTimes = 0;
		for(PedestrianIdKey key : pedestrianEvacuationTimeProcessor.getKeys()) {
			Double evacTime = pedestrianEvacuationTimeProcessor.getValue(key);

			Double maximalEvacTime = getAttributes().getMaximalEvacuationTime();
			Double minimalEvacTime = getAttributes().getMinimalEvacuationTime();

			if((evacTime == Double.POSITIVE_INFINITY && maximalEvacTime != Double.POSITIVE_INFINITY) ||
					(evacTime < minimalEvacTime|| evacTime > maximalEvacTime)) {
				invalidEvacuationTimes++;
			}
		}

		handleAssertion(invalidEvacuationTimes <= 0);
	}

	@Override
	public AttributesTestPedestrianEvacuationTimeProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesTestNumberOverlapsProcessor());
		}

		return (AttributesTestPedestrianEvacuationTimeProcessor)super.getAttributes();
	}
}
