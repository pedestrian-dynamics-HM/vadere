package org.vadere.util.triangulation.triangulator;

import org.vadere.util.geometry.mesh.inter.*;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Set;

/**
 * Created by bzoennchen on 16.01.18.
 */
public class RuppertsTrinangulator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulator{

    private final ITriangulation<P, V, E, F> triangulation;
    private final IMesh<P, V, E, F> mesh;
    private final Set<P> points;

    /**
     *
     *
     * @param triangulation the triangulation which will be manipulated
     * @param mesh          the mesh which describes the geometry
     * @param points        a set of additional points, this set might be empty
     */
    public RuppertsTrinangulator(final ITriangulation<P, V, E, F> triangulation, final IMesh<P, V, E, F> mesh, final Set<P> points) {
        this.triangulation = triangulation;
        this.mesh = mesh;
        this.points = points;
    }

    @Override
    public void generate() {
        triangulation.init();
        triangulation.insert(points);

        
    }
}
