package org.vadere.simulator.projects.dataprocessing_mtp;

public class TimestepPedestrianIdDataKey implements Comparable<TimestepPedestrianIdDataKey> {
	private final int timestep;
	private final int pedestrianId;

	public TimestepPedestrianIdDataKey(int timestep, int pedestrianId) {
		this.timestep = timestep;
		this.pedestrianId = pedestrianId;
	}

	public Integer getTimestep() {
		return timestep;
	}

	public Integer getPedestrianId() {
		return pedestrianId;
	}

	@Override
	public int compareTo(TimestepPedestrianIdDataKey o) {
		int result = Integer.compare(timestep, o.timestep);
		if (result == 0) {
			return Integer.compare(pedestrianId, o.pedestrianId);
		}
		return result;
	}

	public static String[] getHeaders() {
		return new String[] { TimestepDataKey.getHeader(), PedestrianIdDataKey.getHeader() };
	}
}
