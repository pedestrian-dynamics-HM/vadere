package org.vadere.util.triangulation.triangulator;

/**
 * Created by bzoennchen on 25.05.18.
 */
public enum SFCDirection {
	FORWARD,
	BACKWARD;

	public SFCDirection next() {
		return this == FORWARD ? BACKWARD : FORWARD;
	}
}
