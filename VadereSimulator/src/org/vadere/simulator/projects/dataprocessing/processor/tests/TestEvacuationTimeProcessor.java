package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.processor.EvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesTestEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesTestNumberOverlapsProcessor;

/**
 * @author Benedikt Zoennchen
 *
 */
@DataProcessorClass()
public class TestEvacuationTimeProcessor extends TestProcessor {

	private EvacuationTimeProcessor evacuationTimeProcessor;

	public TestEvacuationTimeProcessor() {
		super("test-evacuationTime");
		setAttributes(new AttributesTestEvacuationTimeProcessor());
	}

	@Override
	public void init(@NotNull final ProcessorManager manager) {
		super.init(manager);
		AttributesTestEvacuationTimeProcessor att = this.getAttributes();
		evacuationTimeProcessor =
				(EvacuationTimeProcessor) manager.getProcessor(att.getEvacuationTimeProcessorId());
	}

	@Override
	protected void doUpdate(@NotNull final SimulationState state) {
		evacuationTimeProcessor.update(state);
	}


	@Override
	public void preLoop(SimulationState state) {
		evacuationTimeProcessor.preLoop(state);
	}

	@Override
	public void postLoop(SimulationState state) {
		evacuationTimeProcessor.postLoop(state);

		int invalidEvacuationTimes = 0;

		Double maximalEvacTime = getAttributes().getMaximalEvacuationTime();
		Double minimalEvacTime = getAttributes().getMinimalEvacuationTime();

		Double evacTime = evacuationTimeProcessor.getValue(NoDataKey.key());

		if((evacTime == Double.POSITIVE_INFINITY && maximalEvacTime != Double.POSITIVE_INFINITY) ||
				(evacTime < minimalEvacTime|| evacTime > maximalEvacTime)) {
			invalidEvacuationTimes++;
		}

		String msg = minimalEvacTime + " <= " + evacTime + "(evacuation time) <= " + maximalEvacTime;
		handleAssertion(invalidEvacuationTimes <= 0, msg);
	}

	@Override
	public AttributesTestEvacuationTimeProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesTestNumberOverlapsProcessor());
		}

		return (AttributesTestEvacuationTimeProcessor)super.getAttributes();
	}
}
