package org.vadere.meshing.mesh.inter.mesh;

/**
 * A face {@link IFace} is a generic 2D region i.e. a polygon consisting of points ({@link IVertex}) connected by {@link IHalfEdge}.
 * Half-edges of a face have to be counter-clockwise oriented.
 * The face might be a boundary face i.e. border or hole.
 *
 * @author Benedikt Zoennchen
 *
 */
public interface IFace {}