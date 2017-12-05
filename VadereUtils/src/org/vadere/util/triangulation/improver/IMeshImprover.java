package org.vadere.util.triangulation.improver;

import org.vadere.util.geometry.mesh.inter.*;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public interface IMeshImprover<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {

    /**
     * returns a collection of triangles i.e. all the faces of the current mesh.
     *
     * @return a collection of triangles i.e. all the faces of the current mesh
     */
    Collection<VTriangle> getTriangles();

    /**
     * improves the current triangulation / mesh.
     *
     */
    void improve();

    /**
     * returns the current triangulation / mesh.
     *
     * @return
     */
    ITriangulation<P, V, E, F> getTriangulation();

}
