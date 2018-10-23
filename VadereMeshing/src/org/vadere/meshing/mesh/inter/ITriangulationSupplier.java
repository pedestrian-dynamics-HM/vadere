package org.vadere.meshing.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Supplier;

/**
 * @author Benedikt Zoennchen
 */
public interface ITriangulationSupplier<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Supplier<ITriangulation<P, V, E, F>> {}
