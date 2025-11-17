package org.vadere.meshing.mesh.inter;

import org.vadere.meshing.mesh.inter.mesh.*;

import java.util.function.Supplier;

/**
 * A {@link Supplier} of {@link IMesh} which gives supply to fresh and empty meshes.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IEmptyMeshSupplier<V extends IVertex, E extends IHalfEdge, F extends IFace> extends Supplier<IMeshWithDataStorage<V, E, F>> {}
