package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOutputFile;

/**
 * @author Mario Teixeira Parente
 *
 */

@OutputFileMap(outputFileClass = TimestepPedestrianIdOutputFile.class)
public class TimestepPedestrianIdKey implements DataKey<TimestepPedestrianIdKey> {
	private final int timestep;
	private final int pedestrianId;

	public TimestepPedestrianIdKey(int timestep, int pedestrianId) {
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
	public int compareTo(TimestepPedestrianIdKey o) {
		int result = Integer.compare(timestep, o.timestep);
		if (result == 0) {
			return Integer.compare(pedestrianId, o.pedestrianId);
		}
		return result;
	}

	public static String[] getHeaders() {
		return new String[] { TimestepKey.getHeader(), PedestrianIdKey.getHeader() };
	}

	@Override
	public String toString() {
		return "TimestepPedestrianIdKey{" +
				"timestep=" + timestep +
				", pedestrianId=" + pedestrianId +
				'}';
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
		TimestepPedestrianIdKey other = (TimestepPedestrianIdKey) obj;
		if (pedestrianId != other.pedestrianId)
			return false;
		if (timestep != other.timestep)
			return false;
		return true;
	}
}
