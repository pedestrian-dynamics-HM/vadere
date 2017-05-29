package org.vadere.util.geometry.mesh.inter;

import org.apache.commons.lang3.RandomUtils;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.impl.PFace;
import org.vadere.util.geometry.mesh.impl.PHalfEdge;
import org.vadere.util.geometry.mesh.impl.PMesh;
import org.vadere.util.geometry.mesh.impl.PVertex;
import org.vadere.util.geometry.mesh.triangulations.BasePointLocator;
import org.vadere.util.geometry.mesh.triangulations.DelaunayHierarchy;
import org.vadere.util.geometry.mesh.triangulations.DelaunayTree;
import org.vadere.util.geometry.mesh.triangulations.IncrementalTriangulation;
import org.vadere.util.geometry.mesh.triangulations.UniformTriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.triangulation.IPointConstructor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ITriangulation<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F>, ITriConnectivity<P, V, E, F> {

	void init();
	void compute();
	void finalize();

	List<V> getSuperVertices();

	Stream<F> streamFaces();
	Set<F> getFaces();
	E insert(final P point);
	void insert(final Collection<P> points);
	void remove(final P point);

	Stream<VTriangle> streamTriangles();

	default Set<VLine> getEdges() {
		IMesh<P, V, E, F> mesh = getMesh();
		return getMesh().getEdges().stream()
				.map(he -> new VLine(new VPoint(mesh.getVertex(mesh.getPrev(he))), new VPoint(mesh.getVertex(he))))
				.collect(Collectors.toSet());
	}

	static <P extends IPoint> ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> createPTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound,
			final IPointConstructor<P> pointConstructor) {
		IncrementalTriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> triangulation = new IncrementalTriangulation<>(bound);
		IMesh<P, PVertex<P>, PHalfEdge<P>, PFace<P>> mesh = new PMesh<>(pointConstructor);
		triangulation.setMesh(mesh);

		IPointLocator<P, PVertex<P>, PHalfEdge<P>, PFace<P>> pointLocator;

		switch (type) {
			case BASE:
				pointLocator = new BasePointLocator<>(triangulation);
				break;
			case DELAUNAY_HIERARCHY:
				Supplier<ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>>> supplier = () -> createPTriangulation(IPointLocator.Type.BASE, bound, pointConstructor);
				pointLocator = new DelaunayHierarchy<>(triangulation, supplier);
				break;
			case DELAUNAY_TREE:
			default:
				pointLocator = new DelaunayTree<>(triangulation);
		}

		triangulation.setPointLocator(pointLocator);
		return triangulation;
	}

	static <P extends IPoint> ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> createPTriangulation(
			final IPointLocator.Type type,
			final Collection<P> points,
			final IPointConstructor<P> pointConstructor) {
		ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> triangulation = createPTriangulation(type, GeometryUtils.bound(points), pointConstructor);
		triangulation.insert(points);
		return triangulation;
	}

	static ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> createPTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound) {
		return createPTriangulation(type, bound, (x, y) -> new VPoint(x, y));
	}

	static <P extends IPoint> UniformTriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> createUnifirmTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound,
			final double minTriangleSideLen,
			final IPointConstructor<P> pointConstructor
	) {

		UniformTriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> triangulation = new UniformTriangulation<>(bound, minTriangleSideLen);
		IMesh<P, PVertex<P>, PHalfEdge<P>, PFace<P>> mesh = new PMesh<>(pointConstructor);
		triangulation.setMesh(mesh);

		IPointLocator<P, PVertex<P>, PHalfEdge<P>, PFace<P>> pointLocator;

		switch (type) {
			case BASE:
				pointLocator = new BasePointLocator<>(triangulation);
				break;
			case DELAUNAY_HIERARCHY:
				Supplier<ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>>> supplier = () -> createPTriangulation(IPointLocator.Type.BASE, bound, pointConstructor);
				pointLocator = new DelaunayHierarchy<>(triangulation, supplier);
				break;
			case DELAUNAY_TREE:
			default:
				pointLocator = new DelaunayTree<>(triangulation);
		}

		triangulation.setPointLocator(pointLocator);
		return triangulation;
	}

	static ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> generateRandomTriangulation(final long numberOfPoints) {

		double min = 0;
		double max = 100;

		Set<VPoint> points = new HashSet<>();

		for(int i = 0; i < numberOfPoints; ++i) {
			double x = RandomUtils.nextDouble(min, max);
			double y = RandomUtils.nextDouble(min, max);

			points.add(new VPoint(x, y));
		}

		IMesh<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> mesh = new PMesh<>(IPointConstructor.pointConstructorVPoint);

		IncrementalTriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> triangulation = new IncrementalTriangulation<>(points);
		triangulation.setMesh(mesh);
		triangulation.setPointLocator(new DelaunayTree<>(triangulation));
		triangulation.compute();

		return triangulation;
	}


}
