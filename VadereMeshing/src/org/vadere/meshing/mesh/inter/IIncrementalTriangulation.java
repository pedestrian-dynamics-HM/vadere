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
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.meshing.mesh.impl.VPTriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.meshing.mesh.gen.IncrementalTriangulation;
import org.vadere.meshing.mesh.triangulation.triangulator.UniformTriangulator;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link IIncrementalTriangulation} is a {@link ITriConnectivity} (operations) and is composed of a mesh {@link IMesh} (data) and
 * a point location algorithm {@link IPointLocator}.
 *
 * @param <P> the type of the points (containers)
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IIncrementalTriangulation<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> extends Iterable<F>, ITriangulation<P>, ITriConnectivity<P, CE, CF, V, E, F>, Cloneable {

	void init();
	void compute();
	void finish();
	void recompute();

	IIncrementalTriangulation<P, CE, CF, V, E, F> clone();

	/**
	 * Returns a list of virtual vertices. Virtual vertices support the construction of the triangulation
	 * but they are not part of the (finished) triangulation.
	 *
	 * @return a list of virtual vertices
	 */
	List<V> getVirtualVertices();

	/**
	 * Returns a list of vertices (excluding virtual vertices) of the current triangulation.
	 *
	 * @return a list of vertices (excluding virtual vertices) of the current triangulation.
	 */
	List<V> getVertices();

	Stream<F> streamFaces();
	Set<F> getFaces();
	E insert(final P point);
	void insert(final Collection<P> points);
	void remove(final P point);

	void setPointLocator(@NotNull final IPointLocator.Type type);

	default Set<VLine> getEdges() {
		return getMesh()
				.streamEdges()
				.filter(getMesh()::isAlive).map(getMesh()::toLine).collect(Collectors.toSet());
	}


	// factory methods

	static VPTriangulation createVPTriangulation(@NotNull final VRectangle bound) {
		VPTriangulation vpTriangulation = new VPTriangulation(bound);
		return vpTriangulation;
	}

	static <P extends IPoint> IIncrementalTriangulation<P, ?, ?, PVertex<P, ?, ?>, PHalfEdge<P, ?, ?>, PFace<P, ?, ?>> createPTriangulation(
			@NotNull final IPointLocator.Type type,
			@NotNull final VRectangle bound,
			@NotNull final IPointConstructor<P> pointConstructor) {
		PMesh<P, ?, ?> mesh = new PMesh<>(pointConstructor);
		return new IncrementalTriangulation<P, Object, Object, PVertex<P, ?, ?>, PHalfEdge<P, Object, Object>, PFace<P, Object, Object>>(mesh, type, bound);
	}

	static <P extends IPoint> IIncrementalTriangulation<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> createPTriangulation(
			@NotNull final VRectangle bound,
			@NotNull final IPointConstructor<P> pointConstructor) {
		IMesh<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> mesh = new PMesh<>(pointConstructor);
		return new IncrementalTriangulation<>(mesh, bound);
	}


	static <P extends IPoint> IIncrementalTriangulation<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> createPTriangulation(
			@NotNull final IPointLocator.Type type,
			@NotNull final IMesh<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>>  mesh) {
		IncrementalTriangulation<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> triangulation = new IncrementalTriangulation<>(mesh, type);
		return triangulation;
	}

	static <P extends IPoint> IIncrementalTriangulation<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> createPTriangulation(
			@NotNull final IMesh<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>>  mesh) {
		IncrementalTriangulation<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> triangulation = new IncrementalTriangulation<>(mesh);
		return triangulation;
	}

	static <P extends IPoint> IIncrementalTriangulation<P, ?, ?, AVertex<P>, AHalfEdge<?>, AFace<?>> createATriangulation(
            final IPointLocator.Type type,
            final VRectangle bound,
            final IPointConstructor<P> pointConstructor) {
		IMesh<P, ?, ?, AVertex<P>, AHalfEdge<?>, AFace<?>> mesh = new AMesh<>(pointConstructor);
		IncrementalTriangulation<P, ?, ?, AVertex<P>, AHalfEdge<?>, AFace<?>> triangulation = new IncrementalTriangulation<>(mesh, type, bound);
        return triangulation;
    }

	static <P extends IPoint, CE, CF> IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> createATriangulation(
			final IPointLocator.Type type,
			final IMesh<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>>  mesh) {
		IncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> triangulation = new IncrementalTriangulation<>(mesh, type);
		return triangulation;
	}

	static <P extends IPoint> IIncrementalTriangulation<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> createPTriangulation(
			final IPointLocator.Type type,
			final Collection<P> points,
			final IPointConstructor<P> pointConstructor) {
		IIncrementalTriangulation<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> triangulation = createPTriangulation(type, GeometryUtils.bound(points), pointConstructor);
		triangulation.insert(points);
		return triangulation;
	}

    static <P extends IPoint> IIncrementalTriangulation<P, ?, ?, AVertex<P>, AHalfEdge<?>, AFace<?>> createATriangulation(
            final IPointLocator.Type type,
            final Collection<P> points,
            final IPointConstructor<P> pointConstructor) {
		IIncrementalTriangulation<P, ?, ?, AVertex<P>, AHalfEdge<?>, AFace<?>> triangulation = createATriangulation(type, GeometryUtils.bound(points), pointConstructor);
        triangulation.insert(points);
        return triangulation;
    }

	static IIncrementalTriangulation<VPoint, ?, ?, PVertex<VPoint>, PHalfEdge<?>, PFace<?>> createPTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound) {
		return createPTriangulation(type, bound, (x, y) -> new VPoint(x, y));
	}


	static <P extends IPoint> IIncrementalTriangulation<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> createUniformTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound,
			final double minTriangleSideLen,
			final IPointConstructor<P> pointConstructor
	) {
		IMesh<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> mesh = new PMesh<>(pointConstructor);
	    IncrementalTriangulation<P, ?, ?, PVertex<P>, PHalfEdge<?>, PFace<?>> triangulation = new IncrementalTriangulation<>(mesh, type, bound);
		return new UniformTriangulator<>(bound, minTriangleSideLen, triangulation).generate();
	}

	static IIncrementalTriangulation<VPoint, ?, ?, PVertex<VPoint>, PHalfEdge<?>, PFace<?>> generateRandomTriangulation(final long numberOfPoints) {

		double min = 0;
		double max = 100;

		Set<VPoint> points = new HashSet<>();

		for(int i = 0; i < numberOfPoints; ++i) {
			double x = RandomUtils.nextDouble(min, max);
			double y = RandomUtils.nextDouble(min, max);

			points.add(new VPoint(x, y));
		}

		IMesh<VPoint, ?, ?, PVertex<VPoint>, PHalfEdge<?>, PFace<?>> mesh = new PMesh<>(IPointConstructor.pointConstructorVPoint);
		IncrementalTriangulation<VPoint, ?, ?, PVertex<VPoint>, PHalfEdge<?>, PFace<?>> triangulation = new IncrementalTriangulation<>(mesh, IPointLocator.Type.DELAUNAY_HIERARCHY, points);
		triangulation.compute();
		return triangulation;
	}

	// TODO: refactor => remove duplicated code!
	static <P extends IPoint>  IPointLocator<P, PVertex<P>, PHalfEdge<P>, PFace<P>> createPPointLocator(
			final IPointLocator.Type type,
			final IIncrementalTriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> triConnectivity,
			final VRectangle bound,
			final IPointConstructor<P> pointConstructor) {

		IPointLocator<P, PVertex<P>, PHalfEdge<P>, PFace<P>> pointLocator;

		switch (type) {
			case BASE:
				pointLocator = new BasePointLocator<>(triConnectivity);
				break;
			case DELAUNAY_HIERARCHY:
				Supplier<IIncrementalTriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>>> supplier = () -> createPTriangulation(IPointLocator.Type.BASE, bound, pointConstructor);
				pointLocator = new DelaunayHierarchy<>(triConnectivity, supplier);
				break;
			case DELAUNAY_TREE:
			default:
				pointLocator = new DelaunayTree<>(triConnectivity);
		}

		return pointLocator;
	}

    static <P extends IPoint>  IPointLocator<P, AVertex<P>, AHalfEdge<P>, AFace<P>> createAPointLocator(
            final IPointLocator.Type type,
            final IIncrementalTriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>> triConnectivity,
            final VRectangle bound,
            final IPointConstructor<P> pointConstructor) {

        IPointLocator<P, AVertex<P>, AHalfEdge<P>, AFace<P>> pointLocator;

        switch (type) {
            case BASE:
                pointLocator = new BasePointLocator<>(triConnectivity);
                break;
            case DELAUNAY_HIERARCHY:
                Supplier<IIncrementalTriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>>> supplier = () -> createATriangulation(IPointLocator.Type.BASE, bound, pointConstructor);
                pointLocator = new DelaunayHierarchy<>(triConnectivity, supplier);
                break;
            case DELAUNAY_TREE:
            default:
                pointLocator = new DelaunayTree<>(triConnectivity);
        }

        return pointLocator;
    }

}
