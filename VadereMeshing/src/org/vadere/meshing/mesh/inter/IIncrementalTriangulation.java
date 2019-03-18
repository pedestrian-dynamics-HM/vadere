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
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenUniformTriangulator;

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
 * @author Benedikt Zoennchen
 *
 * @param <P> the type of the points (containers)
 * @param <CE> the type of container of the half-edges
 * @param <CF> the type of the container of the faces
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IIncrementalTriangulation<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> extends Iterable<F>, ITriangulation<P>, ITriConnectivity<P, CE, CF, V, E, F>, Cloneable {

	void init();
	void compute();
	void finish();
	void recompute();
	void addTriEventListener(@NotNull final ITriEventListener<P, CE, CF, V, E, F> triEventListener);
	void removeTriEventListener(@NotNull final ITriEventListener<P, CE, CF, V, E, F> triEventListener);

	IIncrementalTriangulation<P, CE, CF, V, E, F> clone();

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
	E insert(final P point);
	E insert(@NotNull V vertex, @NotNull F face);
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

	static <P extends IPoint, CE, CF> IIncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> createPTriangulation(
			@NotNull final IPointLocator.Type type,
			@NotNull final VRectangle bound,
			@NotNull final IPointConstructor<P> pointConstructor) {
		PMesh<P, CE, CF> mesh = new PMesh<>(pointConstructor);
		return new IncrementalTriangulation<>(mesh, type, bound);
	}

	static <P extends IPoint, CE, CF> IIncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> createPTriangulation(
			@NotNull final VRectangle bound,
			@NotNull final IPointConstructor<P> pointConstructor) {
		IMesh<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> mesh = new PMesh<>(pointConstructor);
		return new IncrementalTriangulation<>(mesh, bound);
	}


	static <P extends IPoint, CE, CF> IIncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> createPTriangulation(
			@NotNull final IPointLocator.Type type,
			@NotNull final IMesh<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>>  mesh) {
		IncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> triangulation = new IncrementalTriangulation<>(mesh, type);
		return triangulation;
	}

	static <P extends IPoint, CE, CF> IIncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> createPTriangulation(
			@NotNull final IMesh<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>>  mesh) {
		IncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> triangulation = new IncrementalTriangulation<>(mesh);
		return triangulation;
	}

	static <P extends IPoint, CE, CF> IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> createATriangulation(
			@NotNull final IPointLocator.Type type,
			@NotNull final VRectangle bound,
			@NotNull final IPointConstructor<P> pointConstructor) {

		IMesh<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> mesh = new AMesh<>(pointConstructor);
		IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> triangulation = new IncrementalTriangulation<>(mesh, type, bound);
		return triangulation;
	}

	static <P extends IPoint, CE, CF> IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> createATriangulation(
			final IPointLocator.Type type,
			final IMesh<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>>  mesh) {
		IncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> triangulation = new IncrementalTriangulation<>(mesh, type);
		return triangulation;
	}

	static <P extends IPoint, CE, CF> IIncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> createPTriangulation(
			final IPointLocator.Type type,
			final Collection<P> points,
			final IPointConstructor<P> pointConstructor) {
		IIncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> triangulation = createPTriangulation(type, GeometryUtils.bound(points), pointConstructor);
		triangulation.insert(points);
		return triangulation;
	}

    static <P extends IPoint, CE, CF> IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> createATriangulation(
            final IPointLocator.Type type,
            final Collection<P> points,
            final IPointConstructor<P> pointConstructor) {
	    IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> triangulation = createATriangulation(type, GeometryUtils.bound(points), pointConstructor);
        triangulation.insert(points);
        return triangulation;
    }

	static <CE, CF> IIncrementalTriangulation<VPoint, CE, CF, PVertex<VPoint, CE, CF>, PHalfEdge<VPoint, CE, CF>, PFace<VPoint, CE, CF>> createPTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound) {
		return createPTriangulation(type, bound, (x, y) -> new VPoint(x, y));
	}


	static <P extends IPoint, CE, CF> IIncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> createUniformTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound,
			final double minTriangleSideLen,
			final IPointConstructor<P> pointConstructor
	) {
		IMesh<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> mesh = new PMesh<>(pointConstructor);
	    IncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> triangulation = new IncrementalTriangulation<>(mesh, type, bound);
		return new GenUniformTriangulator<>(bound, minTriangleSideLen, triangulation).generate();
	}

	static <CE, CF> IIncrementalTriangulation<VPoint, CE, CF, PVertex<VPoint, CE, CF>, PHalfEdge<VPoint, CE, CF>, PFace<VPoint, CE, CF>> generateRandomTriangulation(final long numberOfPoints) {

		double min = 0;
		double max = 100;

		Set<VPoint> points = new HashSet<>();

		for(int i = 0; i < numberOfPoints; ++i) {
			double x = RandomUtils.nextDouble(min, max);
			double y = RandomUtils.nextDouble(min, max);

			points.add(new VPoint(x, y));
		}

		IMesh<VPoint, CE, CF, PVertex<VPoint, CE, CF>, PHalfEdge<VPoint, CE, CF>, PFace<VPoint, CE, CF>> mesh = new PMesh<>(IPointConstructor.pointConstructorVPoint);
		IncrementalTriangulation<VPoint, CE, CF, PVertex<VPoint, CE, CF>, PHalfEdge<VPoint, CE, CF>, PFace<VPoint, CE, CF>> triangulation = new IncrementalTriangulation<>(mesh, IPointLocator.Type.DELAUNAY_HIERARCHY, points);
		triangulation.compute();
		return triangulation;
	}

	// TODO: refactor => remove duplicated code!
	static <P extends IPoint, CE, CF>  IPointLocator<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> createPPointLocator(
			final IPointLocator.Type type,
			final IIncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> triConnectivity,
			final VRectangle bound,
			final IPointConstructor<P> pointConstructor) {

		IPointLocator<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>> pointLocator;

		switch (type) {
			case BASE:
				pointLocator = new BasePointLocator<>(triConnectivity);
				break;
			case DELAUNAY_HIERARCHY:
				Supplier<IIncrementalTriangulation<P, CE, CF, PVertex<P, CE, CF>, PHalfEdge<P, CE, CF>, PFace<P, CE, CF>>> supplier = () -> createPTriangulation(IPointLocator.Type.BASE, bound, pointConstructor);
				pointLocator = new DelaunayHierarchy<>(triConnectivity, supplier);
				break;
			case DELAUNAY_TREE:
			default:
				pointLocator = new DelaunayTree<>(triConnectivity);
		}

		return pointLocator;
	}

    static <P extends IPoint, CE, CF>  IPointLocator<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> createAPointLocator(
            final IPointLocator.Type type,
            final IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> triConnectivity,
            final VRectangle bound,
            final IPointConstructor<P> pointConstructor) {

        IPointLocator<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>> pointLocator;

        switch (type) {
            case BASE:
                pointLocator = new BasePointLocator<>(triConnectivity);
                break;
            case DELAUNAY_HIERARCHY:
                Supplier<IIncrementalTriangulation<P, CE, CF, AVertex<P>, AHalfEdge<CE>, AFace<CF>>> supplier = () -> createATriangulation(IPointLocator.Type.BASE, bound, pointConstructor);
                pointLocator = new DelaunayHierarchy<>(triConnectivity, supplier);
                break;
            case DELAUNAY_TREE:
            default:
                pointLocator = new DelaunayTree<>(triConnectivity);
        }

        return pointLocator;
    }

}
