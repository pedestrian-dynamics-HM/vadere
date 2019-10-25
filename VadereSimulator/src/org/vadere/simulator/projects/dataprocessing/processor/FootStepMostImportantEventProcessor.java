package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;

import java.util.Collection;

/**
 * Save most important event of a pedestrian in each time step in an own column.
 */
@DataProcessorClass()
public class FootStepMostImportantEventProcessor extends DataProcessor<EventtimePedestrianIdKey, String> {

	public FootStepMostImportantEventProcessor() {
		super("mostImportantEvent");
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> pedestrians = state.getTopography().getElements(Pedestrian.class);

		for(Pedestrian p : pedestrians){
			VTrajectory traj = p.getTrajectory();
			String mostImportantEvent = p.getMostImportantEvent().toStringForOutputProcessor();
			for(FootStep fs : traj.getFootSteps()){
				this.putValue(new EventtimePedestrianIdKey(fs.getStartTime(), p.getId()), mostImportantEvent);
			}
		}
	}

}
