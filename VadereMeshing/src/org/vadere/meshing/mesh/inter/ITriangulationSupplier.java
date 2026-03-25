package org.vadere.meshing.mesh.inter;

import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

import java.util.function.Supplier;

/**
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface ITriangulationSupplier<V extends IVertex, E extends IHalfEdge, F extends IFace> extends Supplier<IIncrementalTriangulation<V, E, F>> {}
