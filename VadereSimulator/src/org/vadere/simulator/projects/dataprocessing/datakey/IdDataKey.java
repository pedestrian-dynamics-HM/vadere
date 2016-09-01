package org.vadere.simulator.projects.dataprocessing.datakey;

/**
 * Data key for custom data aggregators when the keys timestep and/or pedestrian
 * ID are not enough.
 * 
 */
public class IdDataKey implements Comparable<IdDataKey> {
	private final int id;

	public IdDataKey(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	@Override
	public int compareTo(IdDataKey o) {
		return Integer.compare(id, o.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdDataKey other = (IdDataKey) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public static String[] getHeaders() {
		return new String[] { "id" };
	}
}
