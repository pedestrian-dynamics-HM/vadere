package org.vadere.simulator.projects.dataprocessing.processor;

import com.google.common.collect.Lists;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Save most important stimulus of a pedestrian in each time step in an own column.
 */
@DataProcessorClass()
public class FootStepMostImportantStimulusProcessor extends DataProcessor<EventtimePedestrianIdKey, String> {

	public static String HEADER = "mostImportantStimulus";

	public FootStepMostImportantStimulusProcessor() {
		super(HEADER);
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> pedestrians = state.getTopography().getElements(Pedestrian.class);

		for(Pedestrian p : pedestrians){
			VTrajectory traj = p.getTrajectory();
			String mostImportantStimulus = p.getMostImportantStimulus().toStringForOutputProcessor();
			SelfCategory selfCat = p.getSelfCategory();

			/* The actions WAIT and CHANGE_TARGET occur in between two footsteps.
			 * With the following if-statement the action is moved to the latest footstep available.
			 * If not, these two self categories are not visualized in the post-visualization.
			 * */
			if ( (selfCat == SelfCategory.WAIT) || (selfCat == SelfCategory.CHANGE_TARGET) ) {
				if (traj.getFootSteps().size() == 0) {
					if (state.getStep() == 1){
						this.putValue(new EventtimePedestrianIdKey(state.getSimTimeInSec(), p.getId()), mostImportantStimulus);
					}
					else {
						this.getData().replace(getKey(p.getId()), mostImportantStimulus);
					}
				}
			}


			for(FootStep fs : traj.getFootSteps()){
				this.putValue(new EventtimePedestrianIdKey(fs.getStartTime(), p.getId()), mostImportantStimulus);
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


}
