package org.vadere.meshing.mesh;

/**
 * Indicates that some operation has led to an illegal mesh.
 * This can mean that the mesh has illegal connectifities or
 * does no longer satisfies some intended properties like being a
 * valid triangulation.
 */
public class IllegalMeshException extends Exception {
	public IllegalMeshException(final String msg) {
		super(msg);
	}
}
