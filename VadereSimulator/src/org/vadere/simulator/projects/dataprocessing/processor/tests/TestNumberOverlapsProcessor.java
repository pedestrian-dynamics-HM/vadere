package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.simulator.projects.dataprocessing.processor.NumberOverlapsProcessor;
import org.vadere.state.attributes.processor.AttributesTestNumberOverlapsProcessor;
import org.vadere.util.logging.Logger;

/**
 * @author Benedikt Zoennchen
 *
 */
@DataProcessorClass()
public class TestNumberOverlapsProcessor extends TestProcessor {

	private static Logger logger = Logger.getLogger(TestNumberOverlapsProcessor.class);
	private NumberOverlapsProcessor overlapProcessor;

	public TestNumberOverlapsProcessor() {
		super("overlap-test");
		setAttributes(new AttributesTestNumberOverlapsProcessor());
	}

	@Override
	public void init(@NotNull final ProcessorManager manager) {
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
	public void preLoop(@NotNull final SimulationState state) {
		overlapProcessor.preLoop(state);
	}

	@Override
	public void postLoop(@NotNull final SimulationState state) {
		overlapProcessor.postLoop(state);
		Long overlaps = overlapProcessor.getValue(NoDataKey.key());
		String msg = overlaps + "(#overlaps) <= " + getAttributes().getMaxOverlaps();
		handleAssertion(overlapProcessor.getValue(NoDataKey.key()) <= getAttributes().getMaxOverlaps(), msg);
	}

	@Override
	public AttributesTestNumberOverlapsProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesTestNumberOverlapsProcessor());
		}

		return (AttributesTestNumberOverlapsProcessor)super.getAttributes();
	}
}
