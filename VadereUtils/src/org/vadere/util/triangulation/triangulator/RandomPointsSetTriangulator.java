package org.vadere.util.triangulation.triangulator;

import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.math.IDistanceFunction;

import java.util.Random;

/**
 * author Benedikt Zoennchen
 *
 * A triangulator which randomly inserts points.
 *
 * @param <P>
 * @param <V>
 * @param <E>
 * @param <F>
 */
public class RandomPointsSetTriangulator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulator<P, V, E, F> {

    private final ITriangulation<P, V, E, F> triangulation;
    private final int numberOfPoints;
    private Random random;
    private final VRectangle bound;
    private final IDistanceFunction distFunc;

    public RandomPointsSetTriangulator(final ITriangulation<P, V, E, F> triangulation,
                                       final int numberOfPoints, final VRectangle bound,
                                       final IDistanceFunction distFunc
                                ) {
        this.triangulation = triangulation;
        this.numberOfPoints = numberOfPoints;
        this.random = new Random();
        this.bound = bound;
        this.distFunc = distFunc;
    }

    @Override
    public ITriangulation<P, V, E, F> generate() {
        triangulation.init();
        int numberOfInsertedPoints = 0;

        while (numberOfInsertedPoints < numberOfPoints) {
            P point = randomPoint();

            if(distFunc.apply(point) <= 0) {
                triangulation.insert(point);
                numberOfInsertedPoints++;
            }
        }

        triangulation.finish();
        return triangulation;
    }

    private P randomPoint() {
        double x = bound.getMinX() + random.nextDouble() * bound.getWidth();
        double y = bound.getMinY() + random.nextDouble() * bound.getHeight();
        return triangulation.getMesh().createPoint(x, y);
    }
}