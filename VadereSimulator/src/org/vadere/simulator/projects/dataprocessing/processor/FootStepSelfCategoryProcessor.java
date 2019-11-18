package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;

import java.util.Collection;

/**
 * Save self category of a pedestrian in each time step in an own column.
 */
@DataProcessorClass()
public class FootStepSelfCategoryProcessor extends DataProcessor<EventtimePedestrianIdKey, String> {

	public static String HEADER = "selfCategory";

	public FootStepSelfCategoryProcessor() {
		super(HEADER);
	}

	@Override
	public void doUpdate(final SimulationState state) {
		Collection<Pedestrian> pedestrians = state.getTopography().getElements(Pedestrian.class);

		for(Pedestrian p : pedestrians){
			VTrajectory traj = p.getTrajectory();
			String selfCategoryString = p.getSelfCategory().toString();
			for(FootStep fs : traj.getFootSteps()){
				this.putValue(new EventtimePedestrianIdKey(fs.getStartTime(), p.getId()), selfCategoryString);
			}
		}
	}

}
