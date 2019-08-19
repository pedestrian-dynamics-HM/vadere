package org.vadere.meshing.mesh.inter;

/**
 * A face {@link IFace} is a generic 2D-face i.e. a polygon consisting of points
 * and is part of the half-edge data structure. A 2D-face is defined by its half-edges {@link IHalfEdge}
 * and their twins {@link IHalfEdge}. Half-edges have to be counter-clockwise oriented.
 *
 * The face might be a boundary face i.e. border or hole.
 *
 * @author Benedikt Zoennchen
 *
 */
public interface IFace {}