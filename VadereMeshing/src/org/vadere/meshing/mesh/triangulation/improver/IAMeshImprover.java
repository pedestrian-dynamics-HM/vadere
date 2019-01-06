package org.vadere.meshing.mesh.triangulation.improver;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 *
 * @param <P> the type of the points (containers)
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IAMeshImprover<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<Object>, F extends IFace<Object>> extends IMeshImprover<P, Object, Object, V, E, F> {}
