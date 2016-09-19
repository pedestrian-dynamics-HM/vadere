package org.vadere.simulator.projects.dataprocessing.datakey;

public class PedestrianIdDataKey implements Comparable<PedestrianIdDataKey> {
	private final int pedestrianId;

	public PedestrianIdDataKey(int pedestrianId) {
		this.pedestrianId = pedestrianId;
	}

	public int getPedestrianId() {
		return pedestrianId;
	}

	public static String getHeader() {
		return "pedestrianId";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + pedestrianId;
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
		PedestrianIdDataKey other = (PedestrianIdDataKey) obj;
		if (pedestrianId != other.pedestrianId)
			return false;
		return true;
	}

	@Override
	public int compareTo(PedestrianIdDataKey o) {
		return Integer.compare(pedestrianId, o.pedestrianId);
	}

	@Override
	public String toString() {
		return Integer.toString(this.pedestrianId);
	}
}
