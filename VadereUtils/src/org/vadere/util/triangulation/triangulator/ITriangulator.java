package org.vadere.util.triangulation.triangulator;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

/**
 * @author Benedikt Zoennchen
 *
 * A triangle generator creates a triangulation using a certain strategy.
 */
@FunctionalInterface
public interface ITriangulator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {
    ITriangulation<P, V, E, F> generate();
}
