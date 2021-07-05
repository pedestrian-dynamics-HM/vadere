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
			for(FootStep fs : traj.getFootSteps()){
				this.putValue(new EventtimePedestrianIdKey(fs.getStartTime(), p.getId()), mostImportantStimulus);
			}
		}
	}

}
