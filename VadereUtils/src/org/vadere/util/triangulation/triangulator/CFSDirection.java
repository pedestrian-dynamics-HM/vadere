package org.vadere.util.triangulation.triangulator;

/**
 * Created by bzoennchen on 25.05.18.
 */
public enum CFSDirection {
	FORWARD,
	BACKWARD;

	public CFSDirection next() {
		return this == FORWARD ? BACKWARD : FORWARD;
	}
}
