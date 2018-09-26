package org.vadere.util.triangulation.triangulator;

/**
 * @author Benedikt Zoennchen
 */
public enum SFCDirection {
	FORWARD,
	BACKWARD;

	public SFCDirection next() {
		return this == FORWARD ? BACKWARD : FORWARD;
	}
}
