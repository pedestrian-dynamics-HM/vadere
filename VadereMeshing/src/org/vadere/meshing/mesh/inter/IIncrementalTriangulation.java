package org.vadere.meshing.mesh.inter;

import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AFace;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AVertex;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles.ATriangleMeshBuilder;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles.PTriangleMeshBuilder;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.meshing.mesh.impl.PTriangulation;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.meshing.mesh.inter.meshConnectivity.ITriConnectivity;
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
 * a point location algorithm {@link ITriangleMeshPointLocator}.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public interface IIncrementalTriangulation<V extends IVertex, E extends IHalfEdge, F extends IFace>
		extends Iterable<F>, ITriangulation<V, E, F>, Cloneable {

	void init();
	void compute();
	void finish();
	void recompute();

	IIncrementalTriangulation<V, E, F> copy();
	IMeshDataStorage<V,E,F> getMeshDataStorage();
	ITriangleMeshBuilder<V,E,F> getMeshBuilder();
	default ITriangleMesh<V,E,F> getMesh(){
		return getMeshBuilder().getMesh();
	}

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

	void setPointLocator(@NotNull final ITriangleMeshPointLocator.Type type);

	void enablePointLocatorCache();

	void disablePointLocatorCache();

	default Set<VLine> getEdges() {
		return getMeshBuilder().getMesh()
				.edges().stream()
				.filter(getMeshBuilder().getMesh().edges()::isAlive).map(getMeshBuilder().getMesh().edges()::toLine).collect(Collectors.toSet());
	}

	default double getInterpolatedValue(final double px, final double py, final String valueName) {
		double x[] = new double[3];
		double y[] = new double[3];
		double z[] = new double[3];
		var face = locateFace(px, py).get();
		getMeshBuilder().getMesh().readConnectivity().getTriPoints(face, x, y, z, valueName, getMeshBuilder().getDataStorage());
		double totalArea = GeometryUtils.areaOfPolygon(x, y);
		double value = InterpolationUtil.barycentricInterpolation(x, y, z, totalArea, px, py);
		return value;
	}


	// factory methods
	static PTriangulation createVPTriangulation(@NotNull final VRectangle bound) {
		PTriangulation pTriangulation = PTriangulation.fromEmptyMesh(bound);
		return pTriangulation;
	}

	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createPTriangulation(
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final VRectangle bound) {
		return IncrementalTriangulation.fromBuilderFactory(PTriangleMeshBuilder::new, type, bound);
	}

	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createPTriangulation(
			@NotNull final VRectangle bound) {
		return IncrementalTriangulation.fromBuilderFactory(PTriangleMeshBuilder::new, bound);
	}

	static <Vertex extends IVertex, Edge extends IHalfEdge, Face extends IFace, Mesh extends ITriangleMesh<Vertex, Edge, Face>> IIncrementalTriangulation<Vertex, Edge, Face>  createTriangulation(
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final ITriangleMeshBuilder<Vertex, Edge, Face> meshBuilder) {
		return IncrementalTriangulation.fromMeshBuilder(meshBuilder, type);
	}

	static IIncrementalTriangulation<AVertex, AHalfEdge, AFace> createATriangulation(
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final VRectangle bound) {
		return IncrementalTriangulation.fromBuilderFactory(ATriangleMeshBuilder::new, type, bound);
	}

	static IIncrementalTriangulation<AVertex, AHalfEdge, AFace> createATriangulation(
			final ITriangleMeshPointLocator.Type type,
			final ITriangleMeshBuilder<AVertex, AHalfEdge, AFace> meshBuilder) {
		return IncrementalTriangulation.fromMeshBuilder(meshBuilder, type);
	}

	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createPTriangulation(
			final ITriangleMeshPointLocator.Type type,
			final Collection<? extends IPoint> points) {
		IIncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = createPTriangulation(type, GeometryUtils.boundRelative(points));
		triangulation.insert(points);
		return triangulation;
	}

    static IIncrementalTriangulation<AVertex, AHalfEdge, AFace> createATriangulation(
		    final ITriangleMeshPointLocator.Type type,
		    final Collection<? extends IPoint> points) {
	    IIncrementalTriangulation<AVertex, AHalfEdge, AFace> triangulation = createATriangulation(type, GeometryUtils.boundRelative(points));
        triangulation.insert(points);
        return triangulation;
    }

	static IIncrementalTriangulation<PVertex, PHalfEdge, PFace> createUniformTriangulation(
			final ITriangleMeshPointLocator.Type type,
			final VRectangle bound,
			final double minTriangleSideLen
	) {
	    IncrementalTriangulation<PVertex, PHalfEdge, PFace> triangulation = IncrementalTriangulation.fromBuilderFactory(PTriangleMeshBuilder::new, type, bound);
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

		var triangulation = IncrementalTriangulation.fromBuilderFactory(PTriangleMeshBuilder::new, ITriangleMeshPointLocator.Type.DELAUNAY_HIERARCHY, points);
		triangulation.compute();
		return triangulation;
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
    static <V extends IVertex, E extends IHalfEdge, F extends IFace, Mesh extends ITriangleMesh<V, E, F>> IIncrementalTriangulation<V, E, F>createBackGroundMesh(
			Supplier<ITriangleMeshBuilder<V, E, F>> meshSupplier,
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
