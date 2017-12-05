package org.vadere.util.triangulation.triangulator;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 *
 * A default triangulator.
 */
public class PointSetTriangulator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulator {

    private final ITriangulation<P, V, E, F> triangulation;
    private final Collection<P> points;

    public PointSetTriangulator(final Collection<P> points, final ITriangulation<P, V, E, F> triangulation) {
        this.triangulation = triangulation;
        this.points = points;
    }

    @Override
    public void generate() {
        triangulation.init();
        triangulation.insert(points);
        triangulation.finalize();
    }
}
