package org.vadere.simulator.projects.dataprocessing_mtp;

import org.apache.commons.lang3.tuple.Pair;

public class TimestepPedestrianIdDataKey extends DataKey<Pair<Integer, Integer>>
		implements Comparable<TimestepPedestrianIdDataKey> {
	public TimestepPedestrianIdDataKey(final Integer timestep, final Integer pedId) {
		super(Pair.of(timestep, pedId));
	}

	public Integer getTimestep() {
		return this.getKey().getLeft();
	}

	public Integer getPedestrianId() {
		return this.getKey().getRight();
	}

	public int compareTo(TimestepPedestrianIdDataKey o) {
		return this.getKey().compareTo(o.getKey());
	}

	public static String getHeader() {
		return TimestepDataKey.getHeader() + " " + PedestrianIdDataKey.getHeader();
	}
}
