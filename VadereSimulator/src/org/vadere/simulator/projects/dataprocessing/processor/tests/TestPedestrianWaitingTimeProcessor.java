package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianWaitingTimeProcessor;
import org.vadere.state.attributes.processor.AttributesTestPedestrianWaitingTimeProcessor;

/**
 * @author Benedikt Zoennchen
 */
@DataProcessorClass()
public class TestPedestrianWaitingTimeProcessor extends TestProcessor {

	private PedestrianWaitingTimeProcessor pedestrianWaitingTimeProcessor;

	public TestPedestrianWaitingTimeProcessor() {
		super("test-pedestrianWaitingTime");
		setAttributes(new AttributesTestPedestrianWaitingTimeProcessor());
	}


	@Override
	public void init(@NotNull final ProcessorManager manager) {
		super.init(manager);
		AttributesTestPedestrianWaitingTimeProcessor att = this.getAttributes();
		pedestrianWaitingTimeProcessor =
				(PedestrianWaitingTimeProcessor) manager.getProcessor(att.getPedestrianWaitingTimeProcessorId());
	}

	@Override
	protected void doUpdate(@NotNull final SimulationState state) {
		pedestrianWaitingTimeProcessor.update(state);
	}

	@Override
	public void preLoop(@NotNull final SimulationState state) {
		pedestrianWaitingTimeProcessor.preLoop(state);
	}

	@Override
	public void postLoop(@NotNull final SimulationState state) {
		pedestrianWaitingTimeProcessor.postLoop(state);

		Double maximalWaitingTime = getAttributes().getMaximalWaitingTime();
		Double minimalWaitingTime = getAttributes().getMinimalWaitingTime();

		int invalidWaitingTimes = 0;
		for(PedestrianIdKey key : pedestrianWaitingTimeProcessor.getKeys()) {
			Double waitingTime = pedestrianWaitingTimeProcessor.getValue(key);

			if((waitingTime == Double.POSITIVE_INFINITY && waitingTime != Double.POSITIVE_INFINITY) ||
					(waitingTime < minimalWaitingTime || waitingTime > maximalWaitingTime)) {
				invalidWaitingTimes++;
			}
		}

		String msg = invalidWaitingTimes + "(#invalid waiting times) <= " + 0;
		handleAssertion(invalidWaitingTimes <= 0, msg);
	}

	@Override
	public AttributesTestPedestrianWaitingTimeProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesTestPedestrianWaitingTimeProcessor());
		}

		return (AttributesTestPedestrianWaitingTimeProcessor)super.getAttributes();
	}
}
