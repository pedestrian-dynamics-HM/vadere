package org.vadere.util.geometry.mesh.inter;


import org.vadere.util.geometry.shapes.IPoint;

/**
 * A half-edge {@link IHalfEdge} is part of a specific face and one part of a full-edge the other
 * part is its twin, i.e. each full-edge consist of 2 half-edges. The twin of a half-edge is
 * the edge of the face neighbouring its face.
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the vertices
 */
public interface IHalfEdge<P extends IPoint> {}
