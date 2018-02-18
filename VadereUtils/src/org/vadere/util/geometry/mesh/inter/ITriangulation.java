package org.vadere.util.geometry.mesh.inter;

import org.apache.commons.lang3.RandomUtils;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.mesh.impl.VPMesh;
import org.vadere.util.geometry.mesh.impl.VPTriangulation;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.triangulation.IncrementalTriangulation;
import org.vadere.util.triangulation.triangulator.UniformTriangulator;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A triangulation is the combination of a mesh (IMesh) and a point locator (IPointLocator).
 *
 * @param <P>
 * @param <V>
 * @param <E>
 * @param <F>
 */
public interface ITriangulation<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F>, ITriConnectivity<P, V, E, F> {

	void init();
	void compute();
	void finish();
	void recompute();

	List<V> getVirtualVertices();

	Stream<F> streamFaces();
	Set<F> getFaces();
	E insert(final P point);
	void insert(final Collection<P> points);
	void remove(final P point);

	Stream<VTriangle> streamTriangles();

	default Set<VLine> getEdges() {
		return getMesh().streamEdges().filter(getMesh()::isAlive).map(getMesh()::toLine).collect(Collectors.toSet());
	}


	// factory methods

	static VPTriangulation createVPTriangulation(final VRectangle bound) {
		VPTriangulation vpTriangulation = new VPTriangulation(bound);
		VPMesh mesh = new VPMesh();
		vpTriangulation.setMesh(mesh);
		vpTriangulation.setPointLocator(createPPointLocator(IPointLocator.Type.DELAUNAY_HIERARCHY, vpTriangulation, bound, (x,y) -> mesh.createPoint(x, y)));
		return vpTriangulation;
	}

	static <P extends IPoint> ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> createPTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound,
			final IPointConstructor<P> pointConstructor) {

		IncrementalTriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> triangulation = new IncrementalTriangulation<>(bound);
		IMesh<P, PVertex<P>, PHalfEdge<P>, PFace<P>> mesh = new PMesh<>(pointConstructor);
		triangulation.setMesh(mesh);

		IPointLocator<P, PVertex<P>, PHalfEdge<P>, PFace<P>> pointLocator = createPPointLocator(type, triangulation, bound, pointConstructor);
		triangulation.setPointLocator(pointLocator);
		return triangulation;
	}


	static <P extends IPoint> ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> createPTriangulation(
			final IPointLocator.Type type,
			final IMesh<P, PVertex<P>, PHalfEdge<P>, PFace<P>>  mesh) {

		IncrementalTriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> triangulation = new IncrementalTriangulation<>(mesh);
		triangulation.setMesh(mesh);

		double minX = mesh.streamPoints().mapToDouble(p -> p.getX()).min().getAsDouble();
		double maxX = mesh.streamPoints().mapToDouble(p -> p.getX()).max().getAsDouble();

		double minY = mesh.streamPoints().mapToDouble(p -> p.getY()).min().getAsDouble();
		double maxY = mesh.streamPoints().mapToDouble(p -> p.getY()).max().getAsDouble();

		IPointLocator<P, PVertex<P>, PHalfEdge<P>, PFace<P>> pointLocator = createPPointLocator(type, triangulation, new VRectangle(minX, minY, maxX-minX, maxY-minY), (x, y) -> mesh.createPoint(x, y));
		triangulation.setPointLocator(pointLocator);
		return triangulation;
	}


	static <P extends IPoint> ITriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>> createATriangulation(
            final IPointLocator.Type type,
            final VRectangle bound,
            final IPointConstructor<P> pointConstructor) {

		IncrementalTriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>> triangulation = new IncrementalTriangulation<>(bound);
        IMesh<P, AVertex<P>, AHalfEdge<P>, AFace<P>> mesh = new AMesh<>(pointConstructor);
        triangulation.setMesh(mesh);

        IPointLocator<P, AVertex<P>, AHalfEdge<P>, AFace<P>> pointLocator = createAPointLocator(type, triangulation, bound, pointConstructor);
        triangulation.setPointLocator(pointLocator);
        return triangulation;
    }


	/**
	 * Creates a triangulation where the bound of it is determined by the mesh
	 *
	 * @param type
	 * @param mesh
	 * @param <P>
	 * @return
	 */
	static <P extends IPoint> ITriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>> createATriangulation(
			final IPointLocator.Type type,
			final IMesh<P, AVertex<P>, AHalfEdge<P>, AFace<P>>  mesh) {

		IncrementalTriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>> triangulation = new IncrementalTriangulation<>(mesh);
		triangulation.setMesh(mesh);

		double minX = mesh.streamPoints().mapToDouble(p -> p.getX()).min().getAsDouble();
		double maxX = mesh.streamPoints().mapToDouble(p -> p.getX()).max().getAsDouble();

		double minY = mesh.streamPoints().mapToDouble(p -> p.getY()).min().getAsDouble();
		double maxY = mesh.streamPoints().mapToDouble(p -> p.getY()).max().getAsDouble();

		IPointLocator<P, AVertex<P>, AHalfEdge<P>, AFace<P>> pointLocator = createAPointLocator(type, triangulation, new VRectangle(minX, minY, maxX-minX, maxY-minY), (x, y) -> mesh.createPoint(x, y));
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

    static <P extends IPoint> ITriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>> createATriangulation(
            final IPointLocator.Type type,
            final Collection<P> points,
            final IPointConstructor<P> pointConstructor) {

		ITriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>> triangulation = createATriangulation(type, GeometryUtils.bound(points), pointConstructor);
        triangulation.insert(points);
        return triangulation;
    }

	static ITriangulation<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> createPTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound) {
		return createPTriangulation(type, bound, (x, y) -> new VPoint(x, y));
	}


	static <P extends IPoint> ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> createUniformTriangulation(
			final IPointLocator.Type type,
			final VRectangle bound,
			final double minTriangleSideLen,
			final IPointConstructor<P> pointConstructor
	) {

	    IncrementalTriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> triangulation = new IncrementalTriangulation<>(bound);
		IMesh<P, PVertex<P>, PHalfEdge<P>, PFace<P>> mesh = new PMesh<>(pointConstructor);
		triangulation.setMesh(mesh);

		IPointLocator<P, PVertex<P>, PHalfEdge<P>, PFace<P>> pointLocator = createPPointLocator(type, triangulation, bound, pointConstructor);
		triangulation.setPointLocator(pointLocator);
		new UniformTriangulator<>(bound, minTriangleSideLen, triangulation).generate();

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

	// TODO: refactor => remove duplicated code!
	static <P extends IPoint>  IPointLocator<P, PVertex<P>, PHalfEdge<P>, PFace<P>> createPPointLocator(
			final IPointLocator.Type type,
			final ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>> triConnectivity,
			final VRectangle bound,
			final IPointConstructor<P> pointConstructor) {

		IPointLocator<P, PVertex<P>, PHalfEdge<P>, PFace<P>> pointLocator;

		switch (type) {
			case BASE:
				pointLocator = new BasePointLocator<>(triConnectivity);
				break;
			case DELAUNAY_HIERARCHY:
				Supplier<ITriangulation<P, PVertex<P>, PHalfEdge<P>, PFace<P>>> supplier = () -> createPTriangulation(IPointLocator.Type.BASE, bound, pointConstructor);
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
            final ITriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>> triConnectivity,
            final VRectangle bound,
            final IPointConstructor<P> pointConstructor) {

        IPointLocator<P, AVertex<P>, AHalfEdge<P>, AFace<P>> pointLocator;

        switch (type) {
            case BASE:
                pointLocator = new BasePointLocator<>(triConnectivity);
                break;
            case DELAUNAY_HIERARCHY:
                Supplier<ITriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>>> supplier = () -> createATriangulation(IPointLocator.Type.BASE, bound, pointConstructor);
                pointLocator = new DelaunayHierarchy<>(triConnectivity, supplier);
                break;
            case DELAUNAY_TREE:
            default:
                pointLocator = new DelaunayTree<>(triConnectivity);
        }

        return pointLocator;
    }

}
