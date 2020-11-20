package org.vadere.simulator.projects.dataprocessing.datakey;

import org.jetbrains.annotations.NotNull;

public class TimestepFaceIdKey implements DataKey<TimestepFaceIdKey> {

	private final int timeStep;
	private final int faceId;

	public TimestepFaceIdKey(final int timeStep, final int faceId) {
		this.timeStep = timeStep;
		this.faceId = faceId;
	}

	public int getTimeStep() {
		return timeStep;
	}

	public int getFaceId() {
		return faceId;
	}

	@Override
	public int compareTo(@NotNull final TimestepFaceIdKey o) {
		int result = Integer.compare(this.timeStep, o.timeStep);

		if (result == 0) {
			result = Integer.compare(this.faceId, o.faceId);
		}

		return result;
	}

	public static String[] getHeaders() {
		return new String[] { TimestepKey.getHeader(), FaceIdKey.getHeader() };
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + faceId;
		result = prime * result + timeStep;
		return result;
	}

	@Override
	public String toString() {
		return "TimestepFaceIdKey{" +
				"timestep=" + timeStep +
				", faceId=" + faceId +
				'}';
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimestepFaceIdKey other = (TimestepFaceIdKey) obj;
		if (timeStep != other.timeStep)
			return false;
		if (faceId != other.faceId)
			return false;
		return true;
	}
}
