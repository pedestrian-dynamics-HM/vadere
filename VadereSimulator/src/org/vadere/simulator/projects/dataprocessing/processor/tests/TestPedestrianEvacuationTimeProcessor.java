package org.vadere.simulator.projects.dataprocessing.processor.tests;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianEvacuationTimeProcessor;
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

		Double maximalEvacTime = getAttributes().getMaximalEvacuationTime();
		Double minimalEvacTime = getAttributes().getMinimalEvacuationTime();

		Double finishTime = state.getScenarioStore().getAttributesSimulation().getFinishTime();

		// See issue #249, this is only for security such that the tests work correctly
		if(finishTime <= maximalEvacTime){
			handleAssertion(false,
					"finishTime in Simulation options has to be larger than maximalEvacTime");
		}

		pedestrianEvacuationTimeProcessor.postLoop(state);

		int invalidEvacuationTimes = 0;
		for(PedestrianIdKey key : pedestrianEvacuationTimeProcessor.getKeys()) {
			Double evacTime = pedestrianEvacuationTimeProcessor.getValue(key);

			if((evacTime == Double.POSITIVE_INFINITY && maximalEvacTime != Double.POSITIVE_INFINITY) ||
					(evacTime < minimalEvacTime || evacTime > maximalEvacTime)) {
				invalidEvacuationTimes++;
			}
		}
		String msg = invalidEvacuationTimes + "(#invalid evacuation times) <= " + 0;
		handleAssertion(invalidEvacuationTimes <= 0, msg);
	}

	@Override
	public AttributesTestPedestrianEvacuationTimeProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesTestPedestrianEvacuationTimeProcessor());
		}

		return (AttributesTestPedestrianEvacuationTimeProcessor)super.getAttributes();
	}
}
