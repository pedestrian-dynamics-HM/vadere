package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Random;

/**
 * <p>A triangulator which randomly inserts points.</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class GenRandomPointsSetTriangulator<V extends IVertex, E extends IHalfEdge, F extends IFace> implements ITriangulator<V, E, F> {

    private final IIncrementalTriangulation<V, E, F> triangulation;
    private final int numberOfPoints;
    private Random random;
    private final VRectangle bound;
    private final IDistanceFunction distFunc;


	/**
	 * The default constructor.
	 *
	 * @param mesh              an empty mesh which will contain the all elements of the triangulation
	 * @param numberOfPoints    the number of random points which will be inserted
	 * @param bound             the bound containing all points
	 * @param distFunc          a distance function which has to be positive at positions where
	 *                          no point should be inserted and negative elsewhere.
	 * @param random            a pseudo random number generator
	 */
	public GenRandomPointsSetTriangulator(@NotNull final IMesh<V, E, F> mesh,
	                                      @NotNull final int numberOfPoints,
	                                      @NotNull final VRectangle bound,
	                                      @NotNull final IDistanceFunction distFunc,
	                                      @NotNull final Random random
	) {
		this.triangulation = new IncrementalTriangulation<>(mesh, bound);
		this.numberOfPoints = numberOfPoints;
		this.random = random;
		this.bound = bound;
		this.distFunc = distFunc;
	}

	/**
	 *
	 * @param mesh              an empty mesh which will contain the all elements of the triangulation
	 * @param numberOfPoints    the number of random points which will be inserted
	 * @param bound             the bound containing all points
	 * @param distFunc          a distance function which has to be positive at positions where
	 *                          no point should be inserted and negative elsewhere.
	 */
    public GenRandomPointsSetTriangulator(@NotNull final IMesh<V, E, F> mesh,
                                          @NotNull final int numberOfPoints,
                                          @NotNull final VRectangle bound,
                                          @NotNull final IDistanceFunction distFunc
                                ) {
        this(mesh, numberOfPoints, bound, distFunc, new Random());
    }

	/**
	 *
	 * @param mesh              an empty mesh which will contain the all elements of the triangulation
	 * @param numberOfPoints    the number of random points which will be inserted
	 * @param bound             the bound containing all points
	 * @param random            a pseudo random number generator
	 */
	public GenRandomPointsSetTriangulator(@NotNull final IMesh<V, E, F> mesh,
	                                      @NotNull final int numberOfPoints,
	                                      @NotNull final VRectangle bound,
	                                      @NotNull final Random random
	) {
		this.triangulation = new IncrementalTriangulation<>(mesh, bound);
		this.numberOfPoints = numberOfPoints;
		this.random = random;
		this.bound = bound;
		this.distFunc = p -> -1.0;
	}

	/**
	 *
	 * @param mesh              an empty mesh which will contain the all elements of the triangulation
	 * @param numberOfPoints    the number of random points which will be inserted
	 * @param bound             the bound containing all points
	 */
	public GenRandomPointsSetTriangulator(@NotNull final IMesh<V, E, F> mesh,
	                                      @NotNull final int numberOfPoints,
	                                      @NotNull final VRectangle bound
	) {
		this(mesh, numberOfPoints, bound, new Random());
	}

    @Override
    public IIncrementalTriangulation<V, E, F> generate() {
		return generate(true);
    }

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		triangulation.init();
		int numberOfInsertedPoints = 0;

		while (numberOfInsertedPoints < numberOfPoints) {
			IPoint point = randomPoint();

			if(distFunc.apply(point) <= 0) {
				triangulation.insert(point);
				numberOfInsertedPoints++;
			}
		}

		if(finalize) {
			triangulation.finish();
		}

		return triangulation;
	}

	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		return triangulation;
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return triangulation.getMesh();
	}

	private IPoint randomPoint() {
        double x = bound.getMinX() + random.nextDouble() * bound.getWidth();
        double y = bound.getMinY() + random.nextDouble() * bound.getHeight();
        return triangulation.getMesh().createPoint(x, y);
    }
}