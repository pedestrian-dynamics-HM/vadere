package org.vadere.simulator.utils.random;

import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.TargetChanger;

import java.util.HashMap;
import java.util.Random;

public class VaderRandom implements VRandom {

	private HashMap<String, MetaSeed> metaSeeds;
	private final Random baseRandom;


	public VaderRandom(final Random random){
		baseRandom = random;
		metaSeeds = new HashMap<>();
		metaSeeds.put(Source.class.getCanonicalName(), new VadereMetaSeed(baseRandom.nextLong()));
		metaSeeds.put(Target.class.getCanonicalName(), new VadereMetaSeed(baseRandom.nextLong()));
		metaSeeds.put(TargetChanger.class.getCanonicalName(), new VadereMetaSeed(baseRandom.nextLong()));
		metaSeeds.put(MeasurementArea.class.getCanonicalName(), new VadereMetaSeed(baseRandom.nextLong()));
		metaSeeds.put(Pedestrian.class.getCanonicalName(), new VadereMetaSeed(baseRandom.nextLong()));
		metaSeeds.put("misc", new VadereMetaSeed(baseRandom.nextLong()));
		metaSeeds.put("traci", new VadereMetaSeed(baseRandom.nextLong()));
	}

	@Override
	public Random get(String metaSeedKey, int id) {
		MetaSeed metaSeed = metaSeeds.get(metaSeedKey);
		if (metaSeed == null){
			throw new IllegalStateException("No metaSeed register for given key " + metaSeedKey);
		}
		return metaSeed.get(id);
	}
}
