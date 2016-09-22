package org.vadere.simulator.projects.dataprocessing.datakey;

/**
 * @author Mario Teixeira Parente
 *
 */

public class TimestepPedestrianIdDataKey implements DataKey<TimestepPedestrianIdDataKey> {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + pedestrianId;
		result = prime * result + timestep;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimestepPedestrianIdDataKey other = (TimestepPedestrianIdDataKey) obj;
		if (pedestrianId != other.pedestrianId)
			return false;
		if (timestep != other.timestep)
			return false;
		return true;
	}
}
