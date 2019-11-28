package org.vadere.simulator.utils.random;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Target;

import java.util.Random;

public interface VRandom {


	/**
	 * The id is NOT unique over all ScenarioElement types.
	 * It is possible to have an Agent with id 5 and a Target with id 5
	 *
	 * If the Random does not exist it will be created.
	 *
	 * @param clazz		class used as key to return Random object
	 * @param id		id of the ScenarioElement
	 * @return			Random object solly used for the {@link ScenarioElement}
	 * 					with the given id.
	 */
	Random get(Class<?> clazz, int id);

	default Random getForPedestrian(int id){
		return get(Pedestrian.class, id);
	}

	default Random getForTarget(int id){
		return get(Target.class, id);
	}

}
