package org.vadere.simulator.projects.dataprocessing.processor;


import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.state.psychology.PsychologyStatus;
import org.vadere.state.psychology.perception.ThreatMemory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;

import java.util.LinkedList;

/**
 * Log {@link Pedestrian}'s current {@link PsychologyStatus} except its {@link ThreatMemory}.
 */
@DataProcessorClass()
public class FootStepPsychologyStatusProcessor extends DataProcessor<EventtimePedestrianIdKey, String> {

	public static String[] HEADERS = { "mostImportantStimulus", "selfCategory", "groupMembership", "informationState" };

	public FootStepPsychologyStatusProcessor() {
		super(HEADERS);
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		for (Pedestrian pedestrian : state.getTopography().getElements(Pedestrian.class)) {
			LinkedList<FootStep> footSteps = pedestrian.getTrajectory().clone().getFootSteps();

			String psychologyStatus = psychologyStatusToString(pedestrian);

			for (FootStep footStep : footSteps) {
				putValue(new EventtimePedestrianIdKey(footStep.getStartTime(), pedestrian.getId()), psychologyStatus);
			}
		}
	}

	private String psychologyStatusToString(Pedestrian pedestrian) {
		String statusAsString = String.format("%s %s %s %s",
				pedestrian.getMostImportantStimulus().toStringForOutputProcessor(),
				pedestrian.getSelfCategory().toString(),
				pedestrian.getGroupMembership().toString(),
				pedestrian.getKnowledgeBase().getInformationState().toString()
				);

		return statusAsString;
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

}