package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.processor.NumberOverlapsProcessor;
import org.vadere.state.attributes.processor.AttributesTestNumberOverlapsProcessor;

/**
 * @author Benedikt Zoennchen
 *
 */
@DataProcessorClass()
public class TestNumberOverlapsProcessor extends TestProcessor {

	private static Logger logger = LogManager.getLogger(TestNumberOverlapsProcessor.class);
	private NumberOverlapsProcessor overlapProcessor;

	public TestNumberOverlapsProcessor() {
		super("overlap-test");
		setAttributes(new AttributesTestNumberOverlapsProcessor());
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesTestNumberOverlapsProcessor att = this.getAttributes();
		overlapProcessor =
				(NumberOverlapsProcessor) manager.getProcessor(att.getNumberOverlapsProcessorId());
	}

	@Override
	protected void doUpdate(@NotNull final SimulationState state) {
		overlapProcessor.update(state);
	}

	@Override
	public void preLoop(SimulationState state) {
		overlapProcessor.preLoop(state);
	}

	@Override
	public void postLoop(SimulationState state) {
		overlapProcessor.postLoop(state);
		handleAssertion(overlapProcessor.getValue(NoDataKey.key()) <= getAttributes().getMaxOverlaps());
	}

	@Override
	public AttributesTestNumberOverlapsProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesTestNumberOverlapsProcessor());
		}

		return (AttributesTestNumberOverlapsProcessor)super.getAttributes();
	}
}
