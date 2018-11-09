package org.vadere.meshing.mesh.triangulation.triangulator;

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
