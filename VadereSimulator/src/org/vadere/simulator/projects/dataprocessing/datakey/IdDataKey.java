package org.vadere.simulator.projects.dataprocessing.datakey;

import org.vadere.simulator.projects.dataprocessing.outputfile.IdOutputFile;

/**
 * Data key for custom data aggregators when the keys timestep and/or pedestrian
 * ID are not enough.
 * 
 */
@OutputFileMap(outputFileClass = IdOutputFile.class)
public class IdDataKey implements DataKey<IdDataKey> {
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		IdDataKey other = (IdDataKey) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public static String[] getHeaders() {
		return new String[] { "id" };
	}
}
