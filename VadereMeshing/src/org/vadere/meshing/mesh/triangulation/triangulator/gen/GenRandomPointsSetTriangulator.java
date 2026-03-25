package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Random;
import java.util.function.Supplier;

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
	 * @param supplier      creates the empty mesh which will contain the all elements of the triangulation
	 * @param numberOfPoints    the number of random points which will be inserted
	 * @param bound             the bound containing all points
	 * @param distFunc          a distance function which has to be positive at positions where
	 *                          no point should be inserted and negative elsewhere.
	 * @param random            a pseudo random number generator
	 */
	public GenRandomPointsSetTriangulator(@NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> supplier,
										  @NotNull final int numberOfPoints,
										  @NotNull final VRectangle bound,
										  @NotNull final IDistanceFunction distFunc,
										  @NotNull final Random random
	) {
		this.triangulation = IncrementalTriangulation.fromBuilderFactory(supplier, bound);
		this.numberOfPoints = numberOfPoints;
		this.random = random;
		this.bound = bound;
		this.distFunc = distFunc;
	}

	/**
	 *
	 * @param supplier      creates the empty mesh which will contain the all elements of the triangulation
	 * @param numberOfPoints    the number of random points which will be inserted
	 * @param bound             the bound containing all points
	 * @param distFunc          a distance function which has to be positive at positions where
	 *                          no point should be inserted and negative elsewhere.
	 */
    public GenRandomPointsSetTriangulator(Supplier<ITriangleMeshBuilder<V, E, F>> supplier,
                                          @NotNull final int numberOfPoints,
                                          @NotNull final VRectangle bound,
                                          @NotNull final IDistanceFunction distFunc
                                ) {
        this(supplier, numberOfPoints, bound, distFunc, new Random());
    }

	/**
	 *
	 * @param supplier      creates the empty mesh which will contain the all elements of the triangulation
	 * @param numberOfPoints    the number of random points which will be inserted
	 * @param bound             the bound containing all points
	 * @param random            a pseudo random number generator
	 */
	public GenRandomPointsSetTriangulator(Supplier<ITriangleMeshBuilder<V, E, F>> supplier,
	                                      @NotNull final int numberOfPoints,
	                                      @NotNull final VRectangle bound,
	                                      @NotNull final Random random
	) {
		this.triangulation = IncrementalTriangulation.fromBuilderFactory(supplier, bound);
		this.numberOfPoints = numberOfPoints;
		this.random = random;
		this.bound = bound;
		this.distFunc = p -> -1.0;
	}

	/**
	 * @param supplier      creates the empty mesh which will contain the all elements of the triangulation
	 * @param numberOfPoints    the number of random points which will be inserted
	 * @param bound             the bound containing all points
	 */
	public GenRandomPointsSetTriangulator(Supplier<ITriangleMeshBuilder<V, E, F>> supplier,
	                                      @NotNull final int numberOfPoints,
	                                      @NotNull final VRectangle bound
	) {
		this(supplier, numberOfPoints, bound, new Random());
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

	public ITriangleMeshBuilder<V, E, F> getMeshBuilder() {
		return triangulation.getMeshBuilder();
	}

	@Override
	public IMeshDataStorage<V, E, F> getMeshDataStorage() {
		return triangulation.getMeshDataStorage();
	}

	private IPoint randomPoint() {
        double x = bound.getMinX() + random.nextDouble() * bound.getWidth();
        double y = bound.getMinY() + random.nextDouble() * bound.getHeight();
        return triangulation.getMesh().createPoint(x, y);
    }
}