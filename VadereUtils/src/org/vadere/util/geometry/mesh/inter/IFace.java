package org.vadere.util.geometry.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

/**
 * A {@link IFace<P>} is a generic 2D-face i.e. a polygon consisting of points of type {@link P}
 * and is part of the half-edge data structure. A 2D-face is defined by its half-edges {@link IHalfEdge<P>}
 * and their twins {@link IHalfEdge<P>}. Half-edges are counter-clockwise oriented.
 *
 * The face might be a boundary face i.e. border or hole.
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the vertices
 */
public interface IFace<P extends IPoint> {}