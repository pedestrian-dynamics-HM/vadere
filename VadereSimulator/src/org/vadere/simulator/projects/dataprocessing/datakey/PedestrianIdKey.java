package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.simulator.projects.dataprocessing.outputfile.PedestrianIdOutputFile;

/**
 * @author Mario Teixeira Parente
 *
 */
@OutputFileMap(outputFileClass = PedestrianIdOutputFile.class)
public class PedestrianIdKey implements DataKey<PedestrianIdKey> {
	private final int pedestrianId;

	public PedestrianIdKey(int pedestrianId) {
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
		PedestrianIdKey other = (PedestrianIdKey) obj;
		if (pedestrianId != other.pedestrianId)
			return false;
		return true;
	}

	@Override
	public int compareTo(PedestrianIdKey o) {
		return Integer.compare(pedestrianId, o.pedestrianId);
	}

	@Override
	public String toString() {
		return Integer.toString(this.pedestrianId);
	}
}
