package org.vadere.simulator.utils.random;

import java.util.HashMap;
import java.util.Random;

public class VadereMetaSeed implements MetaSeed {

	private final Random metaRnd;
	private HashMap<Integer, Random> randomSource;
	private int maxId;

	VadereMetaSeed(long metaSeed){
		this.metaRnd = new Random(metaSeed);
		this.randomSource = new HashMap<>();
		this.maxId = -1;
	}


	@Override
	public Random get(int id) {
		Random rnd = randomSource.get(id);
		if (rnd == null){
			if (id > this.maxId){
				// no Random object exist for given id and id is bigger than any other id previously seen.
				randomSource.put(id, new Random(metaRnd.nextLong()));
				rnd = randomSource.get(id);
				maxId = id;
			} else {
				// a smaller id was given but no object exist for this. This is an error !
				throw new IllegalArgumentException("Non existing Id must allays be greater than existing id.");
			}
		}
		return rnd;
	}
}
