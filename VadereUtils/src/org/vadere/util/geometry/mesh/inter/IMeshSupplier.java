package org.vadere.util.geometry.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Supplier;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 * @param <V>
 * @param <E>
 * @param <F>
 */
public interface IMeshSupplier <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Supplier<IMesh<P, V, E, F>> {}
