package org.vadere.util.geometry.mesh.triangulation.triangulator;

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
