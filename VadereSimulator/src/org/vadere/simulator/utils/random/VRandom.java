package org.vadere.simulator.utils.random;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;

import java.util.Random;

public interface VRandom {


	/**
	 * The id is NOT unique over all ScenarioElement types.
	 * It is possible to have an Agent with id 5 and a Target with id 5
	 *
	 * If the Random does not exist it will be created.
	 *
	 * @param metaSeedKey	Key to select dedicated MetaSeed
	 * @param id		id used within MetaSeed to select the correct Random Object
	 * @return			Random object
	 */

	Random get(String metaSeedKey, int id);

	default Random get(Class<?> clazz, int id){
		return get(clazz.getCanonicalName(), id);
	}

	default Random getForPedestrian(int id){
		return get(Pedestrian.class, id);
	}

	default Random getForTarget(int id){
		return get(Target.class, id);
	}

}
