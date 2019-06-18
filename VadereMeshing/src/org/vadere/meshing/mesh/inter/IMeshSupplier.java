package org.vadere.meshing.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

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
public interface IMeshSupplier <V extends IVertex, E extends IHalfEdge, F extends IFace> extends Supplier<IMesh<V, E, F>> {}
