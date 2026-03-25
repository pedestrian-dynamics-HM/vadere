package org.vadere.meshing.mesh.gen;

import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AFace;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PMesh;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.gen.pointLocator.*;
import org.vadere.meshing.mesh.inter.*;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshEdges;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshFaces;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshVertices;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyTriConnectivity;
import org.vadere.meshing.mesh.inter.meshConnectivity.ITriConnectivity;
import org.vadere.meshing.mesh.iterators.FaceIterator;
import org.vadere.meshing.mesh.triangulation.BowyerWatsonSlow;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenConstrainedDelaunayTriangulator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.*;

/**
 * This class implements the Bowyer-Watson algorithm efficiently by using the mesh data structure {@link IMesh} and
 * sophisticated point locators {@link ITriangleMeshPointLocator} where {@link JumpAndWalkPointLocator} is the default. The incremental nature
 * of the implementation allows to insertVertex points after the triangulation is finished, i.e. after the virtual points and
 * their neighbouring faces are removed. However, points have to lie inside some interior face of the current triangulation.
 * Furthermore, this implementation allows for other criteria {@link Predicate} for flipping edges {@link E} than the
 * Delaunay criterion, e.g. a more relaxed version. However it is only guaranteed to generate a valid triangulation if
 * the Delaunay criterion is used. Otherwise, the user has to make sure that the triangulation remains valid.
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 *
 * @see <a href="https://en.wikipedia.org/wiki/Delaunay_triangulation">Delaunay triangulation</a>
 * @see <a href="https://en.wikipedia.org/wiki/Bowyer%E2%80%93Watson_algorithm">Bowyer-Watson algorithm</a>
 */
public class IncrementalTriangulation<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IIncrementalTriangulation<V, E, F>, ITriEventListener<V, E, F> {

	protected Collection<IPoint> points;
	private VRectangle bound;
	private boolean finalized = false;
	private ITriangleMeshBuilder<V, E, F> meshBuilder;

	private ITriangleMesh<V, E, F> mesh;
	private ITriangleMeshVertices<V,E,F> vertices;
	private ITriangleMeshEdges<V,E,F> edges;
	private ITriangleMeshFaces<V,E,F> faces;

	private ITriangleMeshPointLocator<V, E, F> pointLocator;
	private boolean initialized;
	private List<V> virtualVertices;
	private boolean useMeshForBound;
	private ITriangleMeshPointLocator.Type type;

	private final double edgeCoincidenceTolerance = GeometryUtils.DOUBLE_EPS;

	private Predicate<E> illegalPredicate;
	private static Logger log = Logger.getLogger(IncrementalTriangulation.class);
	private ITriConnectivity<V, E, F> changeConnectivity;
	private IReadOnlyTriConnectivity<V, E, F> readConnectivity;

	/*static {
		ITriConnectivity.log.setDebug();
	}*/

	public ITriangleMesh<V, E, F> getMesh() {
		return mesh;
	}

	/**
	 * Construct a triangulation using an empty mesh.
	 *
	 * @param meshBuilderFactory the empty mesh
	 * @param type              the type of the point location algorithm
	 * @param points            points to be inserted, which also specify the bounding box
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	protected IncrementalTriangulation(
			@NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> meshBuilderFactory,
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final Collection<IPoint> points,
			@NotNull final Predicate<E> illegalPredicate) {

		this.type = type;
		this.useMeshForBound = false;
		this.meshBuilder = meshBuilderFactory.get();
		this.meshBuilder.changeConnectivity().setIsIllegalPredicate(
				evPair -> isIllegal(evPair.getLeft(), evPair.getRight()),
				evDoubleTriple -> isIllegal(evDoubleTriple.getLeft(), evDoubleTriple.getMiddle(), evDoubleTriple.getRight()));
		this.changeConnectivity = meshBuilder.changeConnectivity();
		this.mesh = this.meshBuilder.getMesh();
		this.readConnectivity = this.mesh.readConnectivity();
		this.mesh.addTriEventListener(this);
		this.vertices = mesh.vertices();
		this.edges = mesh.edges();
		this.faces = mesh.faces();
		assert this.vertices.getAll().size() == 0;

		this.points = points;
		this.illegalPredicate = illegalPredicate;
		this.bound = GeometryUtils.boundRelative(points);
		this.finalized = false;
		this.initialized = false;
		this.setPointLocator(type);
	}

	/**
	 * Construct a triangulation using an empty mesh.
	 *
	 * @param meshBuilderFactory              the empty mesh
	 * @param type              the type of the point location algorithm
	 * @param bound             the bound of the triangulation, i.e. there will be no points outside the bound to be inserted into the triangulation
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	protected IncrementalTriangulation(
			@NotNull final Supplier<ITriangleMeshBuilder<V, E, F> > meshBuilderFactory,
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final VRectangle bound,
			@NotNull final Predicate<E> illegalPredicate) {

		this.type = type;
		this.useMeshForBound = false;
		this.meshBuilder = meshBuilderFactory.get();
		this.meshBuilder.changeConnectivity().setIsIllegalPredicate(
				evPair -> isIllegal(evPair.getLeft(), evPair.getRight()),
				evDoubleTriple -> isIllegal(evDoubleTriple.getLeft(), evDoubleTriple.getMiddle(), evDoubleTriple.getRight()));
		this.changeConnectivity = meshBuilder.changeConnectivity();
		this.mesh = this.meshBuilder.getMesh();
		this.mesh.addTriEventListener(this);
		this.readConnectivity = this.mesh.readConnectivity();
		this.vertices = mesh.vertices();
		this.edges = mesh.edges();
		this.faces = mesh.faces();
		assert this.vertices.getAll().size() == 0;

		this.points = new HashSet<>();
		this.illegalPredicate = illegalPredicate;
		this.bound = bound;
		this.finalized = false;
		this.initialized = false;
		this.setPointLocator(type);
	}

	/**
	 * Construct a triangulation using an empty mesh.
	 *
	 * @param meshBuilderFactory factory for a new mesh builder
	 * @param type              the type of the point location algorithm
	 * @param points            points to be inserted, which also specify the bounding box
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	public static <Vert extends IVertex, Edge extends IHalfEdge, Face extends IFace> IncrementalTriangulation<Vert, Edge, Face>  fromBuilderFactory(
			@NotNull final Supplier<ITriangleMeshBuilder<Vert, Edge, Face>> meshBuilderFactory,
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final Collection<IPoint> points,
			@NotNull final Predicate<Edge> illegalPredicate) {
		return new IncrementalTriangulation<>(meshBuilderFactory, type, points, illegalPredicate);
	}

	/**
	 * Construct a triangulation using an empty mesh.
	 *
	 * @param meshBuilderFactory factory for a new mesh builder
	 * @param type              the type of the point location algorithm
	 * @param points            points to be inserted, which also specify the bounding box
	 */
	public static <Vert extends IVertex, Edge extends IHalfEdge, Face extends IFace> IncrementalTriangulation<Vert, Edge, Face>  fromBuilderFactory(
			@NotNull final Supplier<ITriangleMeshBuilder<Vert, Edge, Face>> meshBuilderFactory,
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final Collection<IPoint> points) {
		return new IncrementalTriangulation<>(meshBuilderFactory, type, points, edge -> true);
	}

	/**
	 * Construct a triangulation using an empty mesh.
	 *
	 * @param meshBuilderFactory factory for a new mesh builder
	 * @param type              the type of the point location algorithm
	 * @param bound the bound of the triangulation, i.e. there will be no points outside the
	 * 	 *              bound to be inserted into the triangulation
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	public static <Vert extends IVertex, Edge extends IHalfEdge, Face extends IFace> IncrementalTriangulation<Vert, Edge, Face>  fromBuilderFactory(
			@NotNull final Supplier<ITriangleMeshBuilder<Vert, Edge, Face>> meshBuilderFactory,
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final VRectangle bound,
			@NotNull final Predicate<Edge> illegalPredicate
	){
		return new IncrementalTriangulation<>(meshBuilderFactory, type, bound, illegalPredicate);
	}

	/**
	 * Construct a triangulation using an empty mesh.
	 *
	 * @param meshBuilderFactory factory for a new mesh builder
	 * @param type              the type of the point location algorithm
	 * @param bound the bound of the triangulation, i.e. there will be no points outside the
	 * 	 *              bound to be inserted into the triangulation
	 */
	public static <Vert extends IVertex, Edge extends IHalfEdge, Face extends IFace> IncrementalTriangulation<Vert, Edge, Face>  fromBuilderFactory(
			@NotNull final Supplier<ITriangleMeshBuilder<Vert, Edge, Face>> meshBuilderFactory,
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final VRectangle bound
	){
		return fromBuilderFactory(meshBuilderFactory, type, bound, halfEdge -> true);
	}

	/**
	 * Construct a triangulation using an empty mesh and {@link JumpAndWalkPointLocator} as point location algorithm.
	 *
	 * @param meshBuilderFactory factory for a new mesh builder
	 * @param bound the bound of the triangulation, i.e. there will be no points outside the
	 *              bound to be inserted into the triangulation
	 */
	public static <Vert extends IVertex, Edge extends IHalfEdge, Face extends IFace> IncrementalTriangulation<Vert, Edge, Face>  fromBuilderFactory(
			@NotNull final Supplier<ITriangleMeshBuilder<Vert, Edge, Face>> meshBuilderFactory,
			@NotNull final VRectangle bound
	){
		return fromBuilderFactory(meshBuilderFactory, ITriangleMeshPointLocator.Type.JUMP_AND_WALK, bound, halfEdge -> true);
	}

	/**
	 * Construct a triangulation using an empty mesh and {@link JumpAndWalkPointLocator} as point location algorithm.
	 *
	 * @param meshBuilderFactory factory for a new mesh builder
	 * @param bound the bound of the triangulation, i.e. there will be no points outside the
	 *              bound to be inserted into the triangulation
	 */
	public static <Vert extends IVertex, Edge extends IHalfEdge, Face extends IFace> IncrementalTriangulation<Vert, Edge, Face>  fromBuilderFactory(
			@NotNull final Supplier<ITriangleMeshBuilder<Vert, Edge, Face>> meshBuilderFactory,
			@NotNull final VRectangle bound,
			@NotNull final Predicate<Edge> illegalCondition
	){
		return fromBuilderFactory(meshBuilderFactory, ITriangleMeshPointLocator.Type.JUMP_AND_WALK, bound, illegalCondition);
	}

	/**
	 * Construct a triangulation using non-empty mesh. The border of the mesh specifies the bound.
	 * Therefore the bound has to specify some polygon and there will be no points inserted outside
	 * the bound i.e. outside the mesh.
	 *
	 * @param meshBuilder the non-empty mesh which will be used and which specifies the bound
	 * @param type              the type of the used point location algorithm
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	private IncrementalTriangulation(
			@NotNull final ITriangleMeshBuilder<V, E, F>  meshBuilder,
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final Predicate<E> illegalPredicate) {

		this.type = type;
		this.useMeshForBound = true;

		this.meshBuilder = meshBuilder;
		this.meshBuilder.changeConnectivity().setIsIllegalPredicate(
				evPair -> isIllegal(evPair.getLeft(), evPair.getRight()),
				evDoubleTriple -> isIllegal(evDoubleTriple.getLeft(), evDoubleTriple.getMiddle(), evDoubleTriple.getRight()));
		this.changeConnectivity = meshBuilder.changeConnectivity();
		this.mesh = meshBuilder.getMesh();
		this.mesh.addTriEventListener(this);
		this.readConnectivity = this.mesh.readConnectivity();
		this.vertices = mesh.vertices();
		this.edges = mesh.edges();
		this.faces = mesh.faces();

		assert vertices.count() >= 3;

		this.points = new HashSet<>();
		this.illegalPredicate = illegalPredicate;
		this.bound = GeometryUtils.boundRelative(meshBuilder.getMesh().faces().getPoints(meshBuilder.getMesh().faces().getOuterBorder()));
		this.initialized = false;
		this.finalized = false;
		this.virtualVertices = new ArrayList<>();
		this.virtualVertices.addAll(meshBuilder.getMesh().vertices().getAll());
		this.setPointLocator(type);
	}

	/**
	 * Copy constructor
	 */
	private IncrementalTriangulation(IncrementalTriangulation<V, E, F> toCopy){
		this.type = toCopy.type;
		this.useMeshForBound = toCopy.useMeshForBound;

		this.meshBuilder = toCopy.meshBuilder.copy();
		this.changeConnectivity = meshBuilder.changeConnectivity();
		this.mesh = meshBuilder.getMesh();
		this.mesh.addTriEventListener(this);
		this.readConnectivity = this.mesh.readConnectivity();

		this.vertices = mesh.vertices();
		this.edges = mesh.edges();
		this.faces = mesh.faces();
		this.meshBuilder.changeConnectivity().setIsIllegalPredicate(
				evPair -> isIllegal(evPair.getLeft(), evPair.getRight()),
				evDoubleTriple -> isIllegal(evDoubleTriple.getLeft(), evDoubleTriple.getMiddle(), evDoubleTriple.getRight()));
		this.illegalPredicate = toCopy.illegalPredicate;
		this.bound = toCopy.bound;
		this.finalized = toCopy.finalized;
		this.initialized = toCopy.initialized;
		this.points = new HashSet<>(toCopy.points);

		List<V> cVirtualVertices = new ArrayList<>();
		for(V v : toCopy.virtualVertices) {
			for(V cV : vertices.getAll()) {
				if(v.getPoint().equals(cV.getPoint())) {
					cVirtualVertices.add(cV);
					break;
				}
			}
		}
		assert cVirtualVertices.size() == toCopy.virtualVertices.size();
		this.virtualVertices = cVirtualVertices;

		/**
		 * The point locator is not cloned but reconstructed. Cloning the Delaunay-Hierarchy or the Delaunay-Tree seems impossible with
		 * respect to the performance. However, the reconstruction is also expensive O(n * log(n)) where n is the number of vertices.
		 */
		setPointLocator(toCopy.pointLocator.getType());
	}

	/**
	 * Construct a triangulation using non-empty mesh. The border of the mesh specifies the bound.
	 * Therefore the bound has to specify some polygon and there will be no points inserted outside
	 * the bound i.e. outside the mesh.
	 *
	 * @param meshWithDataStorage the non-empty mesh which will be used and which specifies the bound
	 * @param type              the type of the used point location algorithm
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	public static <Vert extends IVertex, Edge extends IHalfEdge, Face extends IFace> IncrementalTriangulation<Vert, Edge, Face>  fromMeshBuilder(
			@NotNull final ITriangleMeshBuilder<Vert, Edge, Face> meshWithDataStorage,
			@NotNull final ITriangleMeshPointLocator.Type type,
			@NotNull final Predicate<Edge> illegalPredicate
	){
		return new IncrementalTriangulation<>(meshWithDataStorage, type, illegalPredicate);
	}

	/**
	 * Construct a triangulation using non-empty mesh and {@link JumpAndWalkPointLocator} as point location algorithm.
	 * The border of the mesh specifies the bound. Therefore the bound has to specify some polygon and
	 * there will be no points inserted outside the bound i.e. outside the mesh.
	 *
	 * @param meshBuilder              the non-empty mesh which will be used and which specifies the bound
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	public static <Vert extends IVertex, Edge extends IHalfEdge, Face extends IFace> IncrementalTriangulation<Vert, Edge, Face>  fromMeshBuilder(
			@NotNull final ITriangleMeshBuilder<Vert, Edge, Face> meshBuilder,
			@NotNull final Predicate<Edge> illegalPredicate
	){
		return IncrementalTriangulation.fromMeshBuilder(meshBuilder, ITriangleMeshPointLocator.Type.JUMP_AND_WALK, illegalPredicate);
	}

	/**
	 * Construct a triangulation using non-empty mesh. The border of the mesh specifies the bound.
	 * Therefore the bound has to specify some polygon and there will be no points inserted outside
	 * the bound i.e. outside the mesh.
	 *
	 * @param meshBuilder      the non-empty mesh which will be used and which specifies the bound
	 * @param type      the type of the used point location algorithm
	 */
	public static <Vert extends IVertex, Edge extends IHalfEdge, Face extends IFace> IncrementalTriangulation<Vert, Edge, Face>  fromMeshBuilder(
			@NotNull final ITriangleMeshBuilder<Vert, Edge, Face> meshBuilder,
			@NotNull final ITriangleMeshPointLocator.Type type
	){
		return IncrementalTriangulation.fromMeshBuilder(meshBuilder, type, halfEdge -> true);
	}

	/**
	 * Construct a triangulation using non-empty mesh and {@link JumpAndWalkPointLocator} as point location algorithm.
	 * The border of the mesh specifies the bound. Therefore the bound has to specify some polygon and
	 * there will be no points inserted outside the bound i.e. outside the mesh.
	 *
	 * @param meshBuilder the non-empty mesh and its datastorage which will be used and which specifies the bound
	 */
	public static <Vert extends IVertex, Edge extends IHalfEdge, Face extends IFace> IncrementalTriangulation<Vert, Edge, Face>  fromMeshBuilder(
			@NotNull final ITriangleMeshBuilder<Vert, Edge, Face> meshBuilder
	){
		return IncrementalTriangulation.fromMeshBuilder(meshBuilder, ITriangleMeshPointLocator.Type.JUMP_AND_WALK, halfEdge -> true);
	}

	// end constructors

	@Override
	public void setCanIllegalPredicate(@NotNull final Predicate<E> illegalPredicate) {
		this.illegalPredicate = illegalPredicate;
	}

	@Override
	public void setPointLocator(@NotNull final ITriangleMeshPointLocator.Type type) {
		switch (type) {
			default:
			case JUMP_AND_WALK:
				pointLocator = new JumpAndWalkPointLocator<>(this.mesh);
				break;
		}
	}

	public void fillHoles(@NotNull final Supplier<ITriangleMeshBuilder<V, E, F>> meshBuilderFactory) {
		for(F hole : faces.getHoles()) {
			List<IPoint> points = faces.getPoints(hole);

			IncrementalTriangulation<V, E, F> incrementalTriangulation
					= IncrementalTriangulation.fromBuilderFactory(meshBuilderFactory, GeometryUtils.boundRelative(points));

			List<VLine> constrians = edges.streamEdgesOf(hole).map(e -> edges.toLine(e)).collect(Collectors.toList());

			// generate a contrained delaunay triangulation
			GenConstrainedDelaunayTriangulator<V, E, F> cdt = new GenConstrainedDelaunayTriangulator<>(incrementalTriangulation, constrians, false, false);
			cdt.generate(true);

			// remove all faces outside the hole
			VPolygon polygon = faces.toPolygon(hole);
			Predicate<F> removePredicate = face -> !polygon.contains(faces.toTriangleMidpoint(face));
			cdt.getMeshBuilder().changeConnectivity().shrinkBorder(removePredicate, true);

			IMesh<V, E, F> holeMesh = incrementalTriangulation.mesh;
			Map<V, V> vertexToVertex = new HashMap<>();
			Map<F, F> faceToFace = new HashMap<>();
			Map<E, E> edgeToEdge = new HashMap<>();

			E edge = edges.getAnyOf(hole);
			VPoint p2 = vertices.toPoint(vertices.getTwin(edge));
			VPoint p1 = vertices.toPoint(vertices.getEndOf(edge));
			E otherEdge = edges.getOf(holeMesh.faces().getOuterBorder(), p1, p2).get();


			List<E> e = edges.getAllOf(edge).stream().map(es -> edges.getTwin(es)).collect(Collectors.toList());
			List<E> otherEdges = holeMesh.edges().getAllOf(otherEdge);
			otherEdges.add(otherEdges.remove(0));
			Collections.reverse(otherEdges);
			assert e.size() == otherEdges.size();

			// copy elements
			for(int i = 0; i < e.size(); i++) {
				E edgeToCopy = e.get(i);
				E otherEdgeToCopy = otherEdges.get(i);
				vertexToVertex.put(holeMesh.vertices().getEndOf(otherEdgeToCopy), vertices.getEndOf(edgeToCopy));
			}

			for(V v : holeMesh.vertices().getAll()) {
				if(!vertices.isAtBoundary(v)) {
					// maybe clone the vertex?
					vertexToVertex.put(v, meshBuilder.vertices().createAndInsert(holeMesh.vertices().getX(v), holeMesh.vertices().getY(v)));
				}
			}

			for(E es : holeMesh.edges().getAll()) {
				V v = holeMesh.vertices().getEndOf(es);
				edgeToEdge.put(es, meshBuilder.edges().createAndInsert(vertexToVertex.get(v)));
			}

			for(F face : holeMesh.faces().getAll()) {
				faceToFace.put(face, meshBuilder.faces().createAndInsert());
			}

			// copy connectivity
			for(E holeEdge : holeMesh.edges().getAll()) {
				E connectedEdge = edgeToEdge.get(holeEdge);
				meshBuilder.edges().setTwin(connectedEdge, edgeToEdge.get(holeMesh.edges().getTwin(holeEdge)));
				meshBuilder.edges().setNext(connectedEdge, edgeToEdge.get(holeMesh.edges().getNext(holeEdge)));
				meshBuilder.edges().setPrev(connectedEdge, edgeToEdge.get(holeMesh.edges().getPrev(holeEdge)));
				meshBuilder.edges().setVertex(connectedEdge, vertexToVertex.get(holeMesh.vertices().getEndOf(holeEdge)));

				if(!holeMesh.faces().isBoundary(holeMesh.faces().getOf(holeEdge))) {
					meshBuilder.edges().setFace(connectedEdge, faceToFace.get(holeMesh.faces().getOf(holeEdge)));
				}
			}

			for(F holeFace : holeMesh.faces().getAll()) {
				F connectedFace = faceToFace.get(holeFace);
				meshBuilder.faces().setEdge(connectedFace, edgeToEdge.get(holeMesh.edges().getAnyOf(holeFace)));
			}

			for(V holeVertex : holeMesh.vertices().getAll()) {
				V connectedVertex = vertexToVertex.get(holeVertex);
				meshBuilder.vertices().setEdge(connectedVertex, edgeToEdge.get(holeMesh.edges().getOf(holeVertex)));
			}

			// merge internal
			for(int i = 0; i < e.size(); i++) {
				E currentEdge = e.get(i);
				E o = edgeToEdge.get(otherEdges.get(i));

				E twin = edges.getTwin(currentEdge);
				V twinVertex = vertices.getEndOf(twin);
				E twinVertexEdge = edges.getOf(twinVertex);
				E oTwin = holeMesh.edges().getTwin(o);

				if(twinVertexEdge.equals(twin)) {
					meshBuilder.vertices().setEdge(twinVertex, oTwin);
				}

				meshBuilder.edges().setTwin(currentEdge, oTwin);
				meshBuilder.edges().destroy(twin);
				meshBuilder.edges().destroy(o);
			}

			// destroy the hole-mesh
			incrementalTriangulation.getMeshBuilder().clear();
			meshBuilder.faces().destroy(hole);
		}
	}

	@Override
	public void enablePointLocatorCache() {
		if(!pointLocator.isCached()) {
			pointLocator = new CachedPointLocator<>(pointLocator, getMesh());
		}
	}

	@Override
	public void disablePointLocatorCache() {
		if(pointLocator.isCached()) {
			pointLocator = pointLocator.getUncachedLocator();
		}
	}

	@Override
	public void init() {
		if(!initialized) {

			if(vertices.count() == 0) {
				double max = Math.max(bound.getWidth(), bound.getHeight());
				double min = Math.min(bound.getWidth(), bound.getHeight());

				double xMin = bound.getMinX();
				double yMin = bound.getMinY();

				double xMax = bound.getMinX() + 2*max;
				double yMax = bound.getMinY() + 2*max;

				V p0 = meshBuilder.vertices().createAndInsert(xMin, yMin);
				V p1 = meshBuilder.vertices().createAndInsert(xMax, yMin);
				V p2 = meshBuilder.vertices().createAndInsert(xMin, yMax);

				// construct super triangle
				F superTriangle = meshBuilder.faces().createFromVertexesInTheMesh(p0, p1, p2);
				F borderFace = faces.getTwin(edges.getAnyOf(superTriangle));
				// end divide the square into 2 triangles

				this.virtualVertices = Arrays.asList(p0, p1, p2);
				this.initialized = true;
			}
			else {
				assert vertices.count() >= 3;
				F borderFace = faces.getOuterBorder();
				// end divide the square into 2 triangles

				this.virtualVertices = vertices.streamVerticesOf(borderFace).collect(Collectors.toList());
				this.initialized = true;
			}
		}
		else {
			log.warn("the second initialization of the " + this.getClass().getSimpleName() + " has no effect.");
		}
	}

	public double getEdgeCoincidenceTolerance() {
		return edgeCoincidenceTolerance;
	}

	@Override
	public List<V> getVirtualVertices() {
		return virtualVertices;
	}

	@Override
	public List<V> getVertices() {
		return vertices.stream().filter(v -> !virtualVertices.contains(v)).collect(Collectors.toList());
	}

	@Override
	public void compute() {
		init();

		// 1. insertVertex points
		for(IPoint p : points) {
			insert(p);
		}

		// 2. remove super triangle
		finish();
	}

    @Override
    public void recompute() {
	    virtualVertices = new ArrayList<>();
        initialized = false;
        finalized = false;
        points = vertices.toPoints();
	    bound = GeometryUtils.boundRelative(points);
        meshBuilder.clear();
	    setPointLocator(type);
        compute();
    }

    @Override
	public E insert(@NotNull final IPoint point, @NotNull F face) {
		return insertVertex(meshBuilder.vertices().create(point), face);
	}

	public E insertVertex(@NotNull final V vertex, @NotNull final F face) {
		return insertVertex(vertex, face, true);
	}


	@Override
	public E insertVertex(@NotNull final V vertex, @NotNull final F face, boolean legalize) {
		if(!initialized) {
			init();
		}

		E edge = getMesh().edges().closestOfFaceTo(face, vertex.getX(), vertex.getY());
		IPoint p1 = getMesh().edges().getMutableEndPoint(getMesh().edges().getPrev(edge));
		IPoint p2 = getMesh().edges().getMutableEndPoint(edge);

		/*
		 * 3 Cases:
		 *      1) point lies on an vertex of a face => ignore the point
		 *      2) point lies on an edge of a face => split the edge
		 *      3) point lies in the interior of the face => split the face (this should be the main case)
		 */
		if(readConnectivity.isClose(vertex.getX(), vertex.getY(), face, edgeCoincidenceTolerance)) {
			return readConnectivity.getCloseEdge(face, vertex.getX(), vertex.getY(), edgeCoincidenceTolerance).get();
		}
		if(GeometryUtils.isOnEdge(p1, p2, vertex, edgeCoincidenceTolerance)) {
			E newEdge = readConnectivity.getAnyEdge(changeConnectivity.splitEdge(vertex, edge, legalize));
			mesh.insertEvent(newEdge);
			return newEdge;
		}
		else {
			assert readConnectivity.faceContains(vertex.getX(), vertex.getY(), face) : face + " does not contain " + vertex;

			E newEdge = changeConnectivity.splitTriangle(face, vertex,  legalize);
			mesh.insertEvent(newEdge);
			return newEdge;
		}
	}

	private boolean contains(@NotNull final IPoint point) {
		double x = point.getX();
		double y = point.getY();
		double x0 = bound.getMinX();
		double y0 = bound.getMinY();
		return (x >= x0 &&
				y >= y0 &&
				x <= x0 + bound.getWidth() &&
				y <= y0 + bound.getHeight());
	}

	public E insert(double x, double y) {
		return insert(mesh.createPoint(x, y));
	}

	@Override
	public E insert(@NotNull final IPoint point) {
		if(!initialized) {
			init();
		}

		if(contains(point)) {
			F face = pointLocator.locatePoint(point);
			return insert(point, face);
		}
		else {
			throw new IllegalArgumentException(point + " is not contained in " + bound);
		}
	}

	@Override
	public E insertVertex(V vertex) {
		return insertVertex(vertex, true);
	}

	@Override
	public E insertVertex(V vertex, boolean legalize) {
		if(!initialized) {
			init();
		}

		if(contains(vertex)) {
			F face = pointLocator.locatePoint(vertex);
			return insertVertex(vertex, face, legalize);
		}
		else {
			throw new IllegalArgumentException(vertex + " is not contained in " + bound);
		}
	}

	@Override
	public void insertVertices(Collection<? extends V> vertices) {
		if(!initialized) {
			init();
		}

		for(V v : vertices) {
			insertVertex(v);
		}
	}

	@Override
	public void insert(final Collection<? extends IPoint> points) {
		if(!initialized) {
			init();
		}

		// 1. insertVertex points
		for(IPoint p : points) {
			insert(p);
		}
	}

	public ITriangleMeshPointLocator<V, E, F> getPointLocator() {
	    return pointLocator;
    }

	@Override
	public boolean isVirtualFace(@NotNull final F face) {
		if(finalized) {
			return false;
		}
		else {
			return getMesh().vertices().streamVerticesOf(face).anyMatch(v -> virtualVertices.contains(v));
		}

	}

	@Override
	public boolean isVirtualEdge(@NotNull final E edge) {
		if(finalized) {
			return false;
		}
		else {
			return virtualVertices.contains(getMesh().vertices().getEndOf(edge))
					|| virtualVertices.contains(getMesh().vertices().getEndOf(getMesh().edges().getPrev(edge)));
		}
	}

	private boolean isVirtualVertex(@NotNull final V v) {
		return virtualVertices.contains(v);
	}

	@Override
	public void finish() {
        if (finalized) {
            return;
        }
        // remove the super triangle properly!
        if(!useMeshForBound) {
            var edges = getMesh().edges();
            var vertices = getMesh().vertices();
            var faces = getMesh().faces();
            // flip all edges
            List<E> toLegalize = new ArrayList<>();
            for(V virtualPoint : virtualVertices) {

                for(E edge : edges.getAllOf(virtualPoint)) {
                    V vertex = vertices.getEndOf(edges.getNext(edge));
                    if(readConnectivity.isLeftOf(vertex.getX(), vertex.getY(), edges.getNext(edges.getTwin(edge)))) {
						changeConnectivity.flip(edge);
                        toLegalize.add(edge);
                    }
                }
            }

            for(V virtualPoint : virtualVertices) {
                if(!vertices.isDestroyed(virtualPoint)) {
                    List<F> faces1 = faces.getAdjacentOf(virtualPoint);
                    faces1.removeIf(f -> faces.isBoundary(f));
                    faces1.forEach(f -> changeConnectivity.removeFaceAtBorder(f, true));
                }
            }

            for(E edge : toLegalize) {
                if(!edges.isDestroyed(edge)) {
					changeConnectivity.legalize(edge);
                }
            }
        }

		this.mesh.removeTriEventListener(this);
        finalized = true;
    }

	public boolean isDeletionOk(final F face) {
		if(getMesh().faces().isDestroyed(face)) {
			return false;
		}

		for(E halfEdge : getMesh().edges().iterableFor(face)) {
			if(getMesh().edges().isBoundary(getMesh().edges().getTwin(halfEdge))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ITriangleMeshBuilder<V, E, F>  getMeshBuilder() {
		return meshBuilder;
	}

	@Override
	public Optional<F> locateFace(final IPoint point) {
		return pointLocator.locate(point);
	}

	@Override
	public Optional<F> locateFace(@NotNull final IPoint point, final Object caller) {
		return pointLocator.locate(point, caller);
	}

	@Override
	public Optional<F> locateFace(@NotNull final double x, final double y, final Object caller) {
		return pointLocator.locate(x, y, caller);
	}

	@Override
	public Optional<F> locateFace(@NotNull final double x, final double y) {
		return pointLocator.locate(x, y);
	}

	@Override
	public Set<F> getFaces() {
		return streamFaces().collect(Collectors.toSet());
	}

	@Override
	public Stream<F> streamFaces() {
		return stream();
	}

	@Override
	public Stream<VTriangle> streamTriangles() {
		return stream().map(f -> faces.toTriangle(f));
	}

	@Override
	public Stream<Triple<IPoint, IPoint, IPoint>> streamTriples() {
		return faces.stream().map(f -> faceToTriple(f));
	}

	@Override
	public Stream<IPoint> streamPoints() {
		return vertices.streamPoints();
	}

	@Override
	public void remove(IPoint point) {
		Optional<F> optFace = locateFace(point);
		if(optFace.isPresent()) {
			F face = optFace.get();
			for(V vertex : mesh.vertices().iterableFor(face)) {
				if(vertices.toMutablePoint(vertex).equals(point)) {
					changeConnectivity.remove(vertex);
					break;
				}
			}
		}
	}

	public Collection<VTriangle> getTriangles() {
		return stream().map(face -> faceToTriangle(face)).collect(Collectors.toSet());
	}

	private Triple<IPoint, IPoint, IPoint> faceToTriple(final F face) {
		List<IPoint> points = faces.getPoints(face);
		assert points.size() == 3;
		IPoint p1 = points.get(0);
		IPoint p2 = points.get(1);
		IPoint p3 = points.get(2);
		return Triple.of(p1, p2, p3);
	}

	private VTriangle faceToTriangle(final F face) {
		List<V> points = edges.streamEdgesOf(face).map(edge -> vertices.getEndOf(edge)).collect(Collectors.toList());
		V p1 = points.get(0);
		V p2 = points.get(1);
		V p3 = points.get(2);
		return new VTriangle(new VPoint(p1.getX(), p1.getY()), new VPoint(p2.getX(), p2.getY()), new VPoint(p3.getX(), p3.getY()));
	}

	/**
	 * Checks if the edge xy of the triangle xyz is illegal with respect to a point p, which is the case if:
	 * The the circumscribed cycle of the triangle triangle xyz contains p.
	 *
	 * Assumption: p = point(next(edge)).
	 *
	 * @param edge  the edge that might be illegal
	 * @param p     point(next(edge))
	 * @return true if the edge with respect to p is illegal, otherwise false
	 */
	public boolean isIllegal(@NotNull final E edge, @NotNull final V p) {
		if(/*!isVirtualVertex(p) && */!edges.isAtBoundary(edge) && illegalPredicate.test(edge)) {
			/*V v1 = vertices.getEndOf(edge);
			V v2 = vertices.getTwinVertex(edge);

			if(isVirtualVertex(v1)) {
				E e = getMesh().getNext(getMesh().getTwin(edge));
				return !isVirtualEdge(e) && isLeftOf(p.getX(), p.getY(), e);
			} else if(isVirtualVertex(v2)) {
				E e = getMesh().getPrev(getMesh().getTwin(edge));
				return !isVirtualEdge(e) && isLeftOf(p.getX(), p.getY(), e);
			} else {*/
				return getMeshBuilder().changeConnectivity().isDelaunayIllegal(edge, p);
			//}
		}

		return false;
		//return isIllegal(edge, p, mesh);
	}

	public boolean isIllegal(@NotNull final E edge, @NotNull final V p, final double eps) {
		return isIllegal(edge, p);
	}

	@Override
	public Iterator<F> iterator() {
		return new FaceIterator(mesh);
	}

	public Stream<F> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	public IncrementalTriangulation<V, E, F> copy() {
		return new IncrementalTriangulation<>(this);
	}

	@Override
	public void postSplitTriangleEvent(F original, F f1, F f2, F f3, V v) {
		pointLocator.postSplitTriangleEvent(original, f1, f2, f3,v );
	}

	@Override
	public void postSplitHalfEdgeEvent(E originalEdge, F original, F f1, F f2, V v) {
		pointLocator.postSplitHalfEdgeEvent(originalEdge, original, f1, f2,v );
	}

	@Override
	public void postFlipEdgeEvent(F f1, F f2) {
		pointLocator.postFlipEdgeEvent(f1, f2);
	}

	@Override
	public void postInsertEvent(V vertex) {
		pointLocator.postInsertEvent(vertex);
	}

	@Override
	public IMeshDataStorage<V, E, F> getMeshDataStorage() {
		return meshBuilder.getDataStorage();
	}



	// TODO: the following code can be deleted, this is only for visual checks
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int height = 1000;
		int width = 1000;
		int max = Math.max(height, width);

		Set<IPoint> points = new HashSet<>();
		/*points.add(new VPoint(20,20));
		points.add(new VPoint(20,40));
		points.add(new VPoint(75,53));
		points.add(new VPoint(80,70));*/

		Random r = new Random(1);
		for(int i=0; i<100; i++) {
			VPoint point = new VPoint(width*r.nextDouble(), height*r.nextDouble());
			points.add(point);
		}

		IPointConstructor<VPoint> pointConstructor =  (x, y) -> new VPoint(x, y);
		long ms = System.currentTimeMillis();

		PMesh mesh = new PMesh();
		IIncrementalTriangulation<PVertex, PHalfEdge, PFace> bw = IIncrementalTriangulation.createPTriangulation(
				ITriangleMeshPointLocator.Type.JUMP_AND_WALK,
				points
		);
		bw.finish();
		System.out.println(System.currentTimeMillis() - ms);
        Set<VLine> edges = bw.getEdges();

		JFrame window = new JFrame();
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window.setBounds(0, 0, max, max);
		window.getContentPane().add(new Lines(edges, points, max));
		window.setVisible(true);


        ms = System.currentTimeMillis();
        IIncrementalTriangulation<AVertex, AHalfEdge, AFace> bw2 = IIncrementalTriangulation.createATriangulation(
                ITriangleMeshPointLocator.Type.JUMP_AND_WALK,
                points
        );
        bw2.finish();
        System.out.println(System.currentTimeMillis() - ms);

        Set<VLine> edges2 = bw2.getEdges();
        JFrame window2 = new JFrame();
        window2.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window2.setBounds(0, 0, max, max);
        window2.getContentPane().add(new Lines(edges2, points, max));
        window2.setVisible(true);

		ms = System.currentTimeMillis();
		BowyerWatsonSlow bw3 = new BowyerWatsonSlow(points);
		bw3.execute();
		Set<VLine> edges3 = bw3.getTriangles().stream()
				.flatMap(triangle -> triangle.streamLines()).collect(Collectors.toSet());
		System.out.println(System.currentTimeMillis() - ms);

		JFrame window3 = new JFrame();
		window3.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window3.setBounds(0, 0, max, max);
		window3.getContentPane().add(new Lines(edges3, points, max));
		window3.setVisible(true);


		/*VRectangle bound = new VRectangle(0, 0, width, height);
		ITriangulation triangulation = ITriangulation.createVPTriangulation(bound);
		VPUniformRefinement uniformRefinement = new VPUniformRefinement(
				triangulation,
				bound,
				Arrays.asList(new VRectangle(200, 200, 100, 200)),
				p -> 10.0);

		uniformRefinement.generate();
		Set<VLine> edges4 = triangulation.getEdges();

		JFrame window4 = new JFrame();
		window4.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		window4.setBounds(0, 0, max, max);
		window4.getContentPane().add(new Lines(edges4, edges4.stream().flatMap(edge -> edge.streamPoints()).collect(Collectors.toSet()), max));
		window4.setVisible(true);*/
	}

	private static class Lines extends JComponent{
		private Set<VLine> edges;
		private Set<IPoint> points;
		private final int max;

		public Lines(final Set<VLine> edges, final Set<IPoint> points, final int max){
			this.edges = edges;
			this.points = points;
			this.max = max;
		}

		public void paint(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setBackground(Color.white);
			g2.setStroke(new BasicStroke(1.0f));
			g2.setColor(Color.black);
			g2.draw(new VRectangle(200, 200, 100, 200));
			g2.setColor(Color.gray);
			//g2.translate(200, 200);
			//g2.scale(0.2, 0.2);

			g2.draw(new VRectangle(200, 200, 100, 200));

			edges.stream().forEach(edge -> {
				Shape k = new VLine(edge.getP1().getX(), edge.getP1().getY(), edge.getP2().getX(), edge.getP2().getY());
				g2.draw(k);
			});

			points.stream().forEach(point -> {
				VCircle k = new VCircle(point.getX(), point.getY(), 1.0);
				g2.draw(k);
			});

		}
	}
}
