package org.vadere.util.geometry.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Supplier;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IMeshSupplier <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Supplier<IMesh<P, V, E, F>> {}
