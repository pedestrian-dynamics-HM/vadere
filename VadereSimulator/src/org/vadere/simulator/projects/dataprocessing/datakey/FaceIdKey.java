package org.vadere.simulator.projects.dataprocessing.datakey;

import org.jetbrains.annotations.NotNull;

/**
 * @author Benedikt Zoennchen
 */
public class FaceIdKey implements DataKey<FaceIdKey> {

	private final int faceId;

	public FaceIdKey(final int faceId) {
		this.faceId = faceId;
	}

	public int getFaceId() {
		return faceId;
	}

	@Override
	public int compareTo(@NotNull final FaceIdKey o) {
		int result = Integer.compare(this.faceId, o.faceId);
		return result;
	}

	public static String getHeader() {
		return "faceId";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FaceIdKey other = (FaceIdKey) obj;
		if (faceId != other.faceId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Integer.toString(this.faceId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + faceId;
		return result;
	}

}
