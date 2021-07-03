package org.vadere.simulator.projects.dataprocessing.processor;

import com.google.common.collect.Lists;
import org.apache.commons.collections.iterators.ReverseListIterator;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.state.psychology.PsychologyStatus;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.ThreatMemory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;

import java.util.*;

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

			SelfCategory selfCat = pedestrian.getSelfCategory();
			String psychologyStatus = psychologyStatusToString(pedestrian);


			/* The actions WAIT and CHANGE_TARGET occur in between two footsteps.
			* With the following if-statement the action is moved to the latest footstep available.
			* If not, these two self categories are not visualized in the post-visualization.
			* */
			if ( (selfCat == SelfCategory.WAIT) || (selfCat == SelfCategory.CHANGE_TARGET) ) {
				if (footSteps.size() == 0) {
					if (state.getStep() == 1){
						this.putValue(new EventtimePedestrianIdKey(state.getSimTimeInSec(), pedestrian.getId()), psychologyStatus);
					}
					else {
						this.getData().replace(getKey(pedestrian.getId()), psychologyStatus);
					}
				}
			}


			for (FootStep footStep : footSteps) {
				putValue(new EventtimePedestrianIdKey(footStep.getStartTime(), pedestrian.getId()), psychologyStatus);
			}
		}
	}

	private EventtimePedestrianIdKey getKey(int pedId) {
		EventtimePedestrianIdKey key = null;
		for (EventtimePedestrianIdKey k : Lists.reverse(new ArrayList<>(this.getKeys()))) {
			if (k.getPedestrianId() == pedId){
				key = k;
				break;
			}
		}
		//TODO check whether we can use stream instead? currenlty not working, but seems to be more effective
		//key = this.getKeys().stream().filter(k -> k.getPedestrianId() == pedId).max(Comparator.comparing(EventtimePedestrianIdKey::getSimtime)).orElseThrow(NoSuchElementException::new);
		return key;
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