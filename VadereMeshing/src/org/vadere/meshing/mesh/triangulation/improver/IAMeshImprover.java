package org.vadere.meshing.mesh.triangulation.improver;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IAMeshImprover<V extends IVertex, E extends IHalfEdge, F extends IFace> extends IMeshImprover<V, E, F> {}
