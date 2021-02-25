package org.vadere.meshing.mesh.inter;

import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.AFace;
import org.vadere.meshing.mesh.gen.AHalfEdge;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.gen.AVertex;
import org.vadere.meshing.mesh.gen.BasePointLocator;
import org.vadere.meshing.mesh.gen.DelaunayHierarchy;
import org.vadere.meshing.mesh.gen.DelaunayTree;
import org.vadere.meshing.mesh.gen.PFace;
import org.vadere.meshing.mesh.gen.PHalfEdge;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.meshing.mesh.gen.PVertex;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.impl.PTriangulation;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenRuppertsTriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenUniformTriangulator;
import org.vadere.util.math.InterpolationUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link IIncrementalTriangulation} is a {@link ITriConnectivity} (operations) and is composed of a mesh {@link IMesh} (data) and
 * a point location algorithm {@link IPointLocator}.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IIncrementalTriangulation<V extends IVertex, E extends IHalfEdge, F extends IFace> extends Iterable<F>, ITriangulation<V, E, F>, ITriConnectivity<V, E, F>, Cloneable {

	void init();
	void compute();
	void finish();
	void recompute();
	void addTriEventListener(@NotNull final ITriEventListener<V, E, F> triEventListener);
	void removeTriEventListener(@NotNull final ITriEventListener<V, E, F> triEventListener);
	void fillHoles(@NotNull final IMeshSupplier<V, E, F> meshSupplier);

	boolean isIllegal(@NotNull E edge, @NotNull V p, double eps);

	IIncrementalTriangulation<V, E, F> clone();

	void setCanIllegalPredicate(@NotNull final Predicate<E> illegalPredicate);

	/**
	 * Returns a list of virtual vertices. Virtual vertices support the construction of the triangulation
	 * but they are not part of the (finished) triangulation.
	 *
	 * @return a list of virtual vertices
	 */
	List<V> getVirtualVertices();

	boolean isVirtualFace(F face);

	boolean isVirtualEdge(@NotNull final E edge);

	/**
	 * Returns a list of vertices (excluding virtual vertices) of the current triangulation.
	 *
	 * @return a list of vertices (excluding virtual vertices) of the current triangulation.
	 */
	List<V> getVertices();

	Stream<F> streamFaces();
	Set<F> getFaces();

	E insert(double x, double y);
	E insert(final IPoint point);

	/**
	 * Inserts a new vertex which is not jet part of the mesh.
	 * @param vertex
	 * @return
	 */
	E insertVertex(final V vertex);
	void insertVertices(final Collection<? extends V> points);
	E insertVertex(@NotNull V vertex, @NotNull F face);
	E insertVertex(@NotNull V vertex, boolean legalize);
	E insertVertex(@NotNull final V vertex, @NotNull final F face, boolean legalize);

	void insert(final Collection<? extends IPoint> points);

	void remove(final IPoint point);

	void setPointLocator(@NotNull final IPointLocator.Type type);

	void enableCache();

	void disableCache();

	default Set<VLine> getEdges() {
		return getMesh()
				.streamEdges()
				.filter(getMesh()::isAlive).map(getMesh()::toLine).collect(Collectors.toSet());
	}

	default double getInterpolatedValue(final double px, final double py, final String valueName) {
		double x[] = new double[3];
		double y[] = new double[3];
		double z[] = new double[3];
		var face = locateFace(px, py).get();
		getTriPoints(face, x, y, z, valueName);
		double totalArea = GeometryUtils.areaOfPolygon(x, y);
		double value = InterpolationUtil.barycentricInterpolation(x, y, z, totalArea, px, py);
		return value;
	}


	// factory methods

	static PTriangulation createVPTriangulation(@NotNull final VRectangle bound) {
		PTriangulation pTriangulation = new PTriangulation(bound);
		return pTriangulation;
	}

	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createPTriangulation(
			@NotNull final IPointLocator.Type type,
			@NotNull final VRectangle bound) {
		PMesh mesh = new PMesh();
		return new IncrementalTriangulation<>(mesh, type, bound);
	}

	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createPTriangulation(
			@NotNull final VRectangle bound) {
		IMesh<PVertex, PHalfEdge, PFace> mesh = new PMesh();
		return new IncrementalTriangulation<>(mesh, bound);
	}


	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createPTriangulation(
			@NotNull final IPointLocator.Type type,
			@NotNull final IMesh<PVertex, PHalfEdge, PFace>  mesh) {
		IncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = new IncrementalTriangulation<>(mesh, type);
		return triangulation;
	}

	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createPTriangulation(
			@NotNull final IMesh<PVertex, PHalfEdge, PFace>  mesh) {
		IncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = new IncrementalTriangulation<>(mesh);
		return triangulation;
	}

	static IIncrementalTriangulation<AVertex, AHalfEdge, AFace> createATriangulation(
			@NotNull final IPointLocator.Type type,
			@NotNull final VRectangle bound) {

		IMesh<AVertex, AHalfEdge, AFace> mesh = new AMesh();
		IIncrementalTriangulation<AVertex, AHalfEdge, AFace> triangulation = new IncrementalTriangulation<>(mesh, type, bound);
		return triangulation;
	}

	static IIncrementalTriangulation<AVertex, AHalfEdge, AFace> createATriangulation(
			final IPointLocator.Type type,
			final IMesh<AVertex, AHalfEdge, AFace> mesh) {
		IncrementalTriangulation<AVertex, AHalfEdge, AFace> triangulation = new IncrementalTriangulation(mesh, type);
		return triangulation;
	}

	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createPTriangulation(
			final IPointLocator.Type type,
			final Collection<? extends IPoint> points) {
		IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = createPTriangulation(type, GeometryUtils.boundRelative(points));
		triangulation.insert(points);
		return triangulation;
	}

    static IIncrementalTriangulation<AVertex, AHalfEdge, AFace> createATriangulation(
		    final IPointLocator.Type type,
		    final Collection<? extends IPoint> points) {
	    IIncrementalTriangulation<AVertex, AHalfEdge, AFace> triangulation = createATriangulation(type, GeometryUtils.boundRelative(points));
        triangulation.insert(points);
        return triangulation;
    }

	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createUniformTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound,
			final double minTriangleSideLen
	) {
		IMesh<PVertex, PHalfEdge, PFace> mesh = new PMesh();
	    IncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = new IncrementalTriangulation<>(mesh, type, bound);
		return new GenUniformTriangulator<>(bound, minTriangleSideLen, triangulation).generate();
	}

	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> generateRandomTriangulation(final long numberOfPoints) {

		double min = 0;
		double max = 100;

		Set<IPoint> points = new HashSet<>();

		for(int i = 0; i < numberOfPoints; ++i) {
			double x = RandomUtils.nextDouble(min, max);
			double y = RandomUtils.nextDouble(min, max);

			points.add(new VPoint(x, y));
		}

		IMesh<PVertex, PHalfEdge, PFace> mesh = new PMesh();
		IncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = new IncrementalTriangulation<>(mesh, IPointLocator.Type.DELAUNAY_HIERARCHY, points);
		triangulation.compute();
		return triangulation;
	}

	// TODO: refactor => remove duplicated code!
	static IPointLocator<PVertex, PHalfEdge, PFace> createPPointLocator(
			final IPointLocator.Type type,
			final IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triConnectivity,
			final VRectangle bound) {

		IPointLocator<PVertex, PHalfEdge, PFace> pointLocator;

		switch (type) {
			case BASE:
				pointLocator = new BasePointLocator<>(triConnectivity);
				break;
			case DELAUNAY_HIERARCHY:
				Supplier<IIncrementalTriangulation<PVertex, PHalfEdge, PFace>> supplier = () -> createPTriangulation(IPointLocator.Type.BASE, bound);
				pointLocator = new DelaunayHierarchy<>(triConnectivity, supplier);
				break;
			case DELAUNAY_TREE:
			default:
				pointLocator = new DelaunayTree<>(triConnectivity);
		}

		return pointLocator;
	}

    static IPointLocator<AVertex, AHalfEdge, AFace> createAPointLocator(
		    final IPointLocator.Type type,
		    final IIncrementalTriangulation<AVertex, AHalfEdge, AFace> triConnectivity,
		    final VRectangle bound) {

        IPointLocator<AVertex, AHalfEdge, AFace> pointLocator;

        switch (type) {
            case BASE:
                pointLocator = new BasePointLocator<>(triConnectivity);
                break;
            case DELAUNAY_HIERARCHY:
                Supplier<IIncrementalTriangulation<AVertex, AHalfEdge, AFace>> supplier = () -> createATriangulation(IPointLocator.Type.BASE, bound);
                pointLocator = new DelaunayHierarchy<>(triConnectivity, supplier);
                break;
            case DELAUNAY_TREE:
            default:
                pointLocator = new DelaunayTree<>(triConnectivity);
        }

        return pointLocator;
    }

	/**
	 * Generates a background mesh using Ruppert's algorithm.
	 *
	 * Assumption there is no angle3D smaller than 60 degree between two contrains.
	 *
	 * @param meshSupplier
	 * @param pslg
	 * @param <V>
	 * @param <E>
	 * @param <F>
	 * @return
	 */
    static <V extends IVertex, E extends IHalfEdge, F extends IFace> IIncrementalTriangulation<V, E, F>createBackGroundMesh(
		    IMeshSupplier<V, E, F> meshSupplier,
		    @NotNull final PSLG pslg,
		    final boolean createHoles) {

	    GenRuppertsTriangulator<V, E, F> ruppertsTriangulator = new GenRuppertsTriangulator<>(
	    		meshSupplier,
			    pslg,
			    0.0,
			    h -> Double.POSITIVE_INFINITY,
			    createHoles
	    );

		return ruppertsTriangulator.generate();
    }
}
