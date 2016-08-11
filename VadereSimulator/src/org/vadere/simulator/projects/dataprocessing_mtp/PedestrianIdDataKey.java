package org.vadere.simulator.projects.dataprocessing_mtp;

public class PedestrianIdDataKey extends DataKey<Integer> implements Comparable<PedestrianIdDataKey> {
	public PedestrianIdDataKey(final Integer key) {
		super(key);
	}

	public int compareTo(PedestrianIdDataKey o) {
		return this.getKey().compareTo(o.getKey());
	}

	public static String getHeader() {
		return "pid";
	}
}
