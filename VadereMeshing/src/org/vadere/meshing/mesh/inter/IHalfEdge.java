package org.vadere.meshing.mesh.inter;



/**
 * A half-edge {@link IHalfEdge} is part of a specific face and one part of a full-edge the other
 * part is its twin, i.e. each full-edge consist of 2 half-edges. The twin of a half-edge is
 * the edge of the face neighbouring its face.
 *
 * @author Benedikt Zoennchen
 *
 * @param <C> container of the half-edge
 */
public interface IHalfEdge<C> {}
