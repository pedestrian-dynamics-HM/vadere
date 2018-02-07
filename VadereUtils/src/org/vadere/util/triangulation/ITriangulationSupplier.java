package org.vadere.util.triangulation;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Supplier;

/**
 * @author Benedikt Zoennchen
 */
public interface ITriangulationSupplier<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Supplier<ITriangulation<P, V, E, F>> {}
