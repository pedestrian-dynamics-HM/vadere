package org.vadere.meshing.mesh.triangulation.triangulator;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.ITriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Random;

/**
 * <p>A triangulator which randomly inserts points.</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> generic type of the point
 * @param <V> generic type of the vertex
 * @param <E> generic type of the half-edge
 * @param <F> generic type of the face
 */
public class RandomPointsSetTriangulator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulator<P, V, E, F> {

    private final ITriangulation<P, V, E, F> triangulation;
    private final int numberOfPoints;
    private Random random;
    private final VRectangle bound;
    private final IDistanceFunction distFunc;

	/**
	 * The default constructor.
	 *
	 * @param triangulation     a triangulation which determines how points will be inserted
	 * @param numberOfPoints    the number of random points which will be inserted
	 * @param bound             the bound containing all points
	 * @param distFunc          a distance function which has to be positive at positions where
	 *                          no point should be inserted and negative elsewhere.
	 */
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