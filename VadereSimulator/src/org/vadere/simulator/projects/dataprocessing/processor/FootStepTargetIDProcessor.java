package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.util.ModelFilter;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.logging.Logger;

import java.util.Collection;
import java.util.LinkedList;

@DataProcessorClass()
public class FootStepTargetIDProcessor extends DataProcessor<EventtimePedestrianIdKey, Integer> implements ModelFilter {
	private static Logger logger = Logger.getLogger(FootStepTargetIDProcessor.class);

	public FootStepTargetIDProcessor(){
		super("targetId");
	}

	@Override
	protected void doUpdate(SimulationState state) {

		for (Pedestrian p : state.getTopography().getElements(Pedestrian.class)) {
			LinkedList<FootStep> footSteps = p.getTrajectory().clone().getFootSteps();
			SelfCategory selfCat = p.getSelfCategory();

			for(FootStep fs : footSteps){
				this.putValue(new EventtimePedestrianIdKey(fs.getStartTime(), p.getId()), !p.hasNextTarget() ? -1 : p.getNextTargetId());
			}

			/* If the actions WAIT and CHANGE_TARGET occur at the beginning, a dummy step is genereated in the FootStepProcessor.class
			 * Add the target id to the dummy footstep
			 * */
			if ((selfCat == SelfCategory.WAIT) || (selfCat == SelfCategory.CHANGE_TARGET)) {
				if (footSteps.size() == 0) {
					if (state.getStep() == 1) {
						this.putValue(new EventtimePedestrianIdKey(state.getSimTimeInSec(), p.getId()), !p.hasNextTarget() ? -1 : p.getNextTargetId());
					}
				}
			}

		}



	}
}
