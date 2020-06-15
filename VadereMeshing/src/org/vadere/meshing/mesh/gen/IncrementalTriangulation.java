package org.vadere.meshing.mesh.gen;

import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IPointConstructor;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.ITriEventListener;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.iterators.EdgeIterator;
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
 * sophisticated point locators {@link IPointLocator} where {@link JumpAndWalk} is the default. The incremental nature
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
public class IncrementalTriangulation<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IIncrementalTriangulation<V, E, F> {

	protected Collection<IPoint> points;
	private VRectangle bound;
	private boolean finalized = false;
	private IMesh<V, E, F> mesh;
	private IPointLocator<V, E, F> pointLocator;
	private boolean initialized;
	private List<V> virtualVertices;
	private boolean useMeshForBound;
	private IPointLocator.Type type;
	private final List<ITriEventListener<V, E, F>> triEventListeners;


	private static double BUFFER_PERCENTAGE = GeometryUtils.DOUBLE_EPS;

	// TODO this epsilon it hard coded!!! => replace it with a user choice
	private double epsilon = 0.0001;
	private double edgeCoincidenceTolerance = GeometryUtils.DOUBLE_EPS;

	private Predicate<E> illegalPredicate;
	private static Logger log = Logger.getLogger(IncrementalTriangulation.class);

	/*static {
		ITriConnectivity.log.setDebug();
	}*/

	/**
	 * Construct a triangulation using an empty mesh.
	 *
	 * @param mesh              the empty mesh
	 * @param type              the type of the point location algorithm
	 * @param points            points to be inserted, which also specify the bounding box
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	public IncrementalTriangulation(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final IPointLocator.Type type,
			@NotNull final Collection<IPoint> points,
			@NotNull final Predicate<E> illegalPredicate) {

		assert mesh.getNumberOfVertices() == 0;
		this.type = type;
		this.useMeshForBound = false;
		this.mesh = mesh;
		this.points = points;
		this.illegalPredicate = illegalPredicate;
		this.bound = GeometryUtils.boundRelative(points);
		this.finalized = false;
		this.initialized = false;
		this.mesh = mesh;
		this.triEventListeners = new ArrayList<>();
		this.setPointLocator(type);
	}

	/**
	 * Construct a triangulation using an empty mesh.
	 *
	 * @param mesh              the empty mesh
	 * @param type              the type of the point location algorithm
	 * @param bound             the bound of the triangulation, i.e. there will be no points outside the bound to be inserted into the triangulation
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	public IncrementalTriangulation(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final IPointLocator.Type type,
			@NotNull final VRectangle bound,
			@NotNull final Predicate<E> illegalPredicate) {

		assert mesh.getNumberOfVertices() == 0;
		this.type = type;
		this.useMeshForBound = false;
		this.mesh = mesh;
		this.points = new HashSet<>();
		this.illegalPredicate = illegalPredicate;
		this.bound = bound;
		this.finalized = false;
		this.initialized = false;
		this.triEventListeners = new ArrayList<>();
		this.setPointLocator(type);
	}

	/**
	 * Construct a triangulation using an empty mesh.
	 *
	 * @param mesh              the empty mesh
	 * @param type              the type of the point location algorithm
	 * @param bound             the bound of the triangulation, i.e. there will be no points outside the bound to be inserted into the triangulation
	 */
	public IncrementalTriangulation(@NotNull final IMesh<V, E, F> mesh,
	                                @NotNull final IPointLocator.Type type,
	                                @NotNull final VRectangle bound) {
		this(mesh, type, bound, halfEdge -> true);
	}

	/**
	 * Construct a triangulation using an empty mesh and {@link JumpAndWalk} as point location algorithm.
	 *
	 * @param mesh  the empty mesh
	 * @param bound the bound of the triangulation, i.e. there will be no points outside the
	 *              bound to be inserted into the triangulation
	 */
	public IncrementalTriangulation(@NotNull final IMesh<V, E, F> mesh,
	                                @NotNull final VRectangle bound) {
		this(mesh, IPointLocator.Type.JUMP_AND_WALK, bound, halfEdge -> true);
	}

	public IncrementalTriangulation(@NotNull final IMesh<V, E, F> mesh,
	                                @NotNull final VRectangle bound,
	                                @NotNull final Predicate<E> illegalCondition) {
		this(mesh, IPointLocator.Type.JUMP_AND_WALK, bound, illegalCondition);
	}

	/**
	 * Construct a triangulation using non-empty mesh. The border of the mesh specifies the bound.
	 * Therefore the bound has to specify some polygon and there will be no points inserted outside
	 * the bound i.e. outside the mesh.
	 *
	 * @param mesh              the non-empty mesh which will be used and which specifies the bound
	 * @param type              the type of the used point location algorithm
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	public IncrementalTriangulation(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final IPointLocator.Type type,
			@NotNull final Predicate<E> illegalPredicate) {

		assert mesh.getNumberOfVertices() >= 3;
		this.type = type;
		this.useMeshForBound = true;
		this.mesh = mesh;
		this.points = new HashSet<>();
		this.illegalPredicate = illegalPredicate;
		this.bound = GeometryUtils.boundRelative(mesh.getPoints(mesh.getBorder()));
		this.initialized = false;
		this.finalized = false;
		this.virtualVertices = new ArrayList<>();
		this.virtualVertices.addAll(mesh.getVertices());
		this.triEventListeners = new ArrayList<>();
		this.setPointLocator(type);
	}

	/**
	 * Construct a triangulation using non-empty mesh and {@link JumpAndWalk} as point location algorithm.
	 * The border of the mesh specifies the bound. Therefore the bound has to specify some polygon and
	 * there will be no points inserted outside the bound i.e. outside the mesh.
	 *
	 * @param mesh              the non-empty mesh which will be used and which specifies the bound
	 * @param illegalPredicate  a predicate which tests if an edge is illegal, i.e. an edge is illegal if it does not
	 *                          fulfill the delaunay criteria and the illegalPredicate
	 */
	public IncrementalTriangulation(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final Predicate<E> illegalPredicate) {
		this(mesh, IPointLocator.Type.JUMP_AND_WALK, illegalPredicate);
	}

	/**
	 * Construct a triangulation using non-empty mesh. The border of the mesh specifies the bound.
	 * Therefore the bound has to specify some polygon and there will be no points inserted outside
	 * the bound i.e. outside the mesh.
	 *
	 * @param mesh      the non-empty mesh which will be used and which specifies the bound
	 * @param type      the type of the used point location algorithm
	 * @param points    points to be inserted, which also specify the bounding box
	 */
	public IncrementalTriangulation(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final IPointLocator.Type type,
			@NotNull final Collection<IPoint> points) {
		this(mesh, type, points, halfEdge -> true);
	}

	/**
	 * Construct a triangulation using non-empty mesh. The border of the mesh specifies the bound.
	 * Therefore the bound has to specify some polygon and there will be no points inserted outside
	 * the bound i.e. outside the mesh.
	 *
	 * @param mesh      the non-empty mesh which will be used and which specifies the bound
	 * @param type      the type of the used point location algorithm
	 */
	public IncrementalTriangulation(
			@NotNull final IMesh<V, E, F> mesh,
			@NotNull final IPointLocator.Type type) {
		this(mesh, type, halfEdge -> true);
	}

	/**
	 * Construct a triangulation using non-empty mesh and {@link JumpAndWalk} as point location algorithm.
	 * The border of the mesh specifies the bound. Therefore the bound has to specify some polygon and
	 * there will be no points inserted outside the bound i.e. outside the mesh.
	 *
	 * @param mesh      the non-empty mesh which will be used and which specifies the bound
	 */
	public IncrementalTriangulation(@NotNull final IMesh<V, E, F> mesh) {
		this(mesh, IPointLocator.Type.JUMP_AND_WALK, halfEdge -> true);
	}

	@Override
	public void setCanIllegalPredicate(@NotNull final Predicate<E> illegalPredicate) {
		this.illegalPredicate = illegalPredicate;
	}

	// end constructors

	@Override
	public void setPointLocator(@NotNull final IPointLocator.Type type) {

		/**
		 * This method is somehow recursive. The Delaunay-Hierarchy is a hierarchy of triangulations using the base point
		 * location algorithm. Therefore if the type is equal to the Delaunay-Hierarchy a supplier is required which
		 * construct triangulations based on this triangulation i.e. with the same "starting" mesh or bound!
		 */
		switch (type) {
			case DELAUNAY_TREE:
				if(!points.isEmpty()) {
					throw new IllegalArgumentException(IPointLocator.Type.DELAUNAY_TREE + " is only supported for empty triangulations.");
				}
				pointLocator = new DelaunayTree<>(this);
				break;
			case DELAUNAY_HIERARCHY:
				Supplier<IIncrementalTriangulation<V, E, F>> supplier;
				if(useMeshForBound) {
					supplier = () -> new IncrementalTriangulation<>(mesh.clone(), IPointLocator.Type.BASE, illegalPredicate);
				}
				else {
					supplier = () -> new IncrementalTriangulation<>(mesh.construct(), IPointLocator.Type.BASE, bound, illegalPredicate);
				}
				pointLocator = new DelaunayHierarchy<>(this, supplier);
				break;
			case JUMP_AND_WALK:
				pointLocator = new JumpAndWalk<>(this);
				break;
			default: pointLocator = new BasePointLocator<>(this);
		}
	}

	public void fillHoles(@NotNull final IMeshSupplier<V, E, F> meshSupplier) {
		for(F hole : getMesh().getHoles()) {
			List<IPoint> points = getMesh().getPoints(hole);
			IncrementalTriangulation<V, E, F> incrementalTriangulation = new IncrementalTriangulation<>(
					meshSupplier.get(),
					IPointLocator.Type.JUMP_AND_WALK,
					GeometryUtils.boundRelative(points),
					e -> true);

			List<VLine> constrians = getMesh().streamEdges(hole).map(e -> getMesh().toLine(e)).collect(Collectors.toList());

			// generate a contrained delaunay triangulation
			GenConstrainedDelaunayTriangulator<V, E, F> cdt = new GenConstrainedDelaunayTriangulator<>(incrementalTriangulation, constrians, false, false);
			cdt.generate(true);

			// remove all faces outside the hole
			VPolygon polygon = getMesh().toPolygon(hole);
			Predicate<F> removePredicate = face -> !polygon.contains(getMesh().toMidpoint(face));
			cdt.getTriangulation().shrinkBorder(removePredicate, true);

			IMesh<V, E, F> holeMesh = incrementalTriangulation.getMesh();
			Map<V, V> vertexToVertex = new HashMap<>();
			Map<F, F> faceToFace = new HashMap<>();
			Map<E, E> edgeToEdge = new HashMap<>();

			E edge = getMesh().getEdge(hole);
			VPoint p2 = getMesh().toPoint(getMesh().getTwinVertex(edge));
			VPoint p1 = getMesh().toPoint(getMesh().getVertex(edge));
			E otherEdge = getMesh().getEdge(holeMesh.getBorder(), p1, p2).get();


			List<E> edges = getMesh().getEdges(edge).stream().map(e -> getMesh().getTwin(e)).collect(Collectors.toList());
			List<E> otherEdges = holeMesh.getEdges(otherEdge);
			otherEdges.add(otherEdges.remove(0));
			Collections.reverse(otherEdges);
			assert edges.size() == otherEdges.size();

			// copy elements
			for(int i = 0; i < edges.size(); i++) {
				E e = edges.get(i);
				E o = otherEdges.get(i);
				vertexToVertex.put(holeMesh.getVertex(o), getMesh().getVertex(e));
			}

			for(V v : holeMesh.getVertices()) {
				if(!getMesh().isAtBoundary(v)) {
					// maybe clone the vertex?
					vertexToVertex.put(v, getMesh().insertVertex(holeMesh.getX(v), holeMesh.getY(v)));
				}
			}

			for(E e : holeMesh.getEdges()) {
				V v = holeMesh.getVertex(e);
				edgeToEdge.put(e, getMesh().createEdge(vertexToVertex.get(v)));
			}

			for(F face : holeMesh.getFaces()) {
				faceToFace.put(face, getMesh().createFace());
			}
			//faceToFace.put(holeMesh.getBorder(), )

			// copy connectivity
			for(E o : holeMesh.getEdges()) {
				E e = edgeToEdge.get(o);
				getMesh().setTwin(e, edgeToEdge.get(holeMesh.getTwin(o)));
				getMesh().setNext(e, edgeToEdge.get(holeMesh.getNext(o)));
				getMesh().setPrev(e, edgeToEdge.get(holeMesh.getPrev(o)));
				getMesh().setVertex(e, vertexToVertex.get(holeMesh.getVertex(o)));

				if(!holeMesh.isBoundary(holeMesh.getFace(o))) {
					getMesh().setFace(e, faceToFace.get(holeMesh.getFace(o)));
				}
			}

			for(F o : holeMesh.getFaces()) {
				F e = faceToFace.get(o);
				getMesh().setEdge(e, edgeToEdge.get(holeMesh.getEdge(o)));
			}

			for(V o : holeMesh.getVertices()) {
				V e = vertexToVertex.get(o);
				getMesh().setEdge(e, edgeToEdge.get(holeMesh.getEdge(o)));
			}

			// merge internal
			for(int i = 0; i < edges.size(); i++) {
				E e = edges.get(i);
				E o = edgeToEdge.get(otherEdges.get(i));

				E twin = getMesh().getTwin(e);
				V tv = getMesh().getVertex(twin);
				E ve = getMesh().getEdge(tv);
				E oTwin = holeMesh.getTwin(o);

				if(ve.equals(twin)) {
					getMesh().setEdge(tv, oTwin);
				}

				getMesh().setTwin(e, oTwin);
				getMesh().destroyEdge(twin);
				getMesh().destroyEdge(o);
			}

			// destroy the hole-mesh
			holeMesh.clear();
			getMesh().destroyFace(hole);
		}
	}

	@Override
	public void enableCache() {
		if(!pointLocator.isCached()) {
			pointLocator = new CachedPointLocator<>(pointLocator, this);
		}
	}

	@Override
	public void disableCache() {
		if(pointLocator.isCached()) {
			pointLocator = pointLocator.getUncachedLocator();
		}
	}

	@Override
	public void init() {
		if(!initialized) {

			if(mesh.getNumberOfVertices() == 0) {
				double max = Math.max(bound.getWidth(), bound.getHeight());
				double min = Math.min(bound.getWidth(), bound.getHeight());


				double xMin = bound.getMinX();
				double yMin = bound.getMinY();

				double xMax = bound.getMinX() + 2*max;
				double yMax = bound.getMinY() + 2*max;

				V p0 = mesh.insertVertex(xMin, yMin);
				V p1 = mesh.insertVertex(xMax, yMin);
				V p2 = mesh.insertVertex(xMin, yMax);

				// construct super triangle
				F superTriangle = mesh.createFace(p0, p1, p2);
				F borderFace = mesh.getTwinFace(mesh.getEdge(superTriangle));
				// end divide the square into 2 triangles

				this.virtualVertices = Arrays.asList(p0, p1, p2);
				this.initialized = true;
			}
			else {
				assert mesh.getNumberOfVertices() >= 3;
				F borderFace = mesh.getBorder();
				// end divide the square into 2 triangles

				this.virtualVertices = mesh.streamVertices(borderFace).collect(Collectors.toList());
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
		return getMesh().streamVertices().filter(v -> !virtualVertices.contains(v)).collect(Collectors.toList());
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
        points = mesh.getPoints();
	    bound = GeometryUtils.boundRelative(points);
        mesh.clear();
	    setPointLocator(type);
        compute();
    }

    @Override
	public E insert(@NotNull final IPoint point, @NotNull F face) {
		return insertVertex(getMesh().createVertex(point), face);
	}

	public E insertVertex(@NotNull final V vertex, @NotNull final F face) {
		return insertVertex(vertex, face, true);
	}


	@Override
	public E insertVertex(@NotNull final V vertex, @NotNull final F face, boolean legalize) {
		if(!initialized) {
			init();
		}

		E edge = mesh.closestEdge(face, vertex.getX(), vertex.getY());
		IPoint p1 = mesh.getPoint(mesh.getPrev(edge));
		IPoint p2 = mesh.getPoint(edge);

		/*
		 * 3 Cases:
		 *      1) point lies on an vertex of a face => ignore the point
		 *      2) point lies on an edge of a face => split the edge
		 *      3) point lies in the interior of the face => split the face (this should be the main case)
		 */
		if(isClose(vertex.getX(), vertex.getY(), face, edgeCoincidenceTolerance)) {
			//log.info("ignore insertion point, since the point " + vertex + " already exists or it is too close to another point!");
			return getCloseEdge(face, vertex.getX(), vertex.getY(), edgeCoincidenceTolerance).get();
		}
		if(GeometryUtils.isOnEdge(p1, p2, vertex, edgeCoincidenceTolerance)) {
			//log.info("splitEdge()");
			E newEdge = getAnyEdge(splitEdge(vertex, edge, legalize));
			insertEvent(newEdge);
			return newEdge;
		}
		else {
			//log.info("splitTriangle()");
			/*if(!contains(vertex.getX(), vertex.getY(), face)) {
				System.out.println("wtf" + contains(vertex.getX(), vertex.getY(), face));
			}*/
			assert contains(vertex.getX(), vertex.getY(), face) : face + " does not contain " + vertex;

			E newEdge = splitTriangle(face, vertex,  legalize);
			insertEvent(newEdge);
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

	protected IPointLocator<V, E, F> getPointLocator() {
	    return pointLocator;
    }

	@Override
	public boolean isVirtualFace(@NotNull final F face) {
		if(finalized) {
			return false;
		}
		else {
			return getMesh().streamVertices(face).anyMatch(v -> virtualVertices.contains(v));
		}

	}

	@Override
	public boolean isVirtualEdge(@NotNull final E edge) {
		if(finalized) {
			return false;
		}
		else {
			return virtualVertices.contains(getMesh().getVertex(edge))
					|| virtualVertices.contains(getMesh().getVertex(getMesh().getPrev(edge)));
		}
	}

	/**
	 * Removes the super triangle from the mesh data structure.
	 */
	/*public void finish() {
		if(!finalized) {
			// we have to use other halfedges than he1 and he2 since they might be deleted
			// if we deleteBoundaryFace he0!
			List<F> faces1 = IteratorUtils.toList(new AdjacentFaceIterator(mesh, he0));
			List<F> faces2 = IteratorUtils.toList(new AdjacentFaceIterator(mesh, he1));
			List<F> faces3 = IteratorUtils.toList(new AdjacentFaceIterator(mesh, he2));

			faces1.removeIf(f -> mesh.isBoundary(f));
			faces1.forEach(f -> deleteBoundaryFace(f));

			faces2.removeIf(f -> mesh.isDestroyed(f) || mesh.isBoundary(f));
			faces2.forEach(f -> deleteBoundaryFace(f));

			faces3.removeIf(f -> mesh.isDestroyed(f) || mesh.isBoundary(f));
			faces3.forEach(f -> deleteBoundaryFace(f));

			finalized = true;
		}
	}*/


	private boolean isVirtualVertex(@NotNull final V v) {
		return virtualVertices.contains(v);
	}


	@Override
	public void finish() {
		if(!finalized) {
			// remove the super triangle properly!
			if(!useMeshForBound) {
				// flip all edges
				List<E> toLegalize = new ArrayList<>();
				for(V virtualPoint : virtualVertices) {

					for(E edge : getMesh().getEdges(virtualPoint)) {
						V vertex = getMesh().getVertex(getMesh().getNext(edge));
						if(isLeftOf(vertex.getX(), vertex.getY(), getMesh().getNext(getMesh().getTwin(edge)))) {
							flip(edge);
							toLegalize.add(edge);
						}
					}
				}

				for(V virtualPoint : virtualVertices) {
					if(!mesh.isDestroyed(virtualPoint)) {
						List<F> faces1 = mesh.getFaces(virtualPoint);
						faces1.removeIf(f -> mesh.isBoundary(f));
						faces1.forEach(f -> removeFaceAtBorder(f, true));
					}
				}

				for(E edge : toLegalize) {
					if(!getMesh().isDestroyed(edge)) {
						legalize(edge);
					}
				}
			}

			//assert getMesh().streamEdges().noneMatch(e -> isIllegal(e));

			/*if(!mesh.isDestroyed(p1)) {
				List<F> faces2 = mesh.getFaces(p1);
				faces2.removeIf(f -> mesh.isDestroyed(f) || mesh.isBoundary(f));
				faces2.forEach(f -> removeFaceAtBoundary(f, true));
			}

			if(!mesh.isDestroyed(p2)) {
				List<F> faces3 = mesh.getFaces(p2);
				faces3.removeIf(f -> mesh.isDestroyed(f) || mesh.isBoundary(f));
				faces3.forEach(f -> removeFaceAtBoundary(f, true));
			}*/

			finalized = true;
		}
	}

	@Override
	public void addTriEventListener(@NotNull ITriEventListener<V, E, F> triEventListener) {
		triEventListeners.add(triEventListener);
	}

	@Override
	public void removeTriEventListener(@NotNull ITriEventListener<V, E, F> triEventListener) {
		triEventListeners.remove(triEventListener);
	}

	public boolean isDeletionOk(final F face) {
		if(mesh.isDestroyed(face)) {
			return false;
		}

		for(E halfEdge : mesh.getEdgeIt(face)) {
			if(mesh.isBoundary(mesh.getTwin(halfEdge))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Deletes a face assuming that the face triangleContains at least one boundary edge, otherwise the
	 * deletion will not result in an feasibly triangulation.
	 *
	 * @param face the face that will be deleted, which as to be adjacent to the boundary.
	 */
	public void deleteBoundaryFace(final F face) {
		//assert isDeletionOk(face);

		// 3 cases: 1. triangle consist of 1, 2 or 3 boundary edges
		List<E> boundaryEdges = new ArrayList<>(3);
		List<E> nonBoundaryEdges = new ArrayList<>(3);

		for(E halfEdge : mesh.getEdgeIt(face)) {
			if(mesh.isBoundary(mesh.getTwin(halfEdge))) {
				boundaryEdges.add(halfEdge);
			}
			else {
				nonBoundaryEdges.add(halfEdge);
			}
		}

		if(boundaryEdges.size() == 3) {
			// release memory
			mesh.getEdges(face).forEach(halfEdge -> mesh.destroyEdge(halfEdge));
		}
		else if(boundaryEdges.size() == 2) {
			E toB = mesh.isBoundary(mesh.getTwin(mesh.getNext(boundaryEdges.get(0)))) ? boundaryEdges.get(0) : boundaryEdges.get(1);
			E toF = mesh.isBoundary(mesh.getTwin(mesh.getNext(boundaryEdges.get(0)))) ? boundaryEdges.get(1) : boundaryEdges.get(0);
			E nB = nonBoundaryEdges.get(0);
			mesh.setFace(nB, mesh.getTwinFace(toF));
			mesh.setNext(nB, mesh.getNext(mesh.getTwin(toB)));
			mesh.setPrev(nB, mesh.getPrev(mesh.getTwin(toF)));
			mesh.setEdge(mesh.getFace(mesh.getTwin(toF)), nB);

			//this.face = mesh.getTwinFace(toF);

			// release memory
			mesh.destroyEdge(toF);
			mesh.destroyEdge(toB);

		}
		else {
			E boundaryHe = boundaryEdges.get(0);
			E prec = mesh.getPrev(mesh.getTwin(boundaryHe));
			E succ = mesh.getNext(mesh.getTwin(boundaryHe));

			E next = mesh.getNext(boundaryHe);
			E prev = mesh.getPrev(boundaryHe);
			mesh.setPrev(next, prec);
			mesh.setFace(next, mesh.getTwinFace(boundaryHe));

			mesh.setNext(prev, succ);
			mesh.setFace(prev, mesh.getTwinFace(boundaryHe));

			mesh.setEdge(mesh.getFace(prec), prec);

			//this.face = mesh.getFace(prec);
			// release memory
			mesh.destroyEdge(boundaryHe);
		}

		mesh.destroyFace(face);
	}

	@Override
	public IMesh<V, E, F> getMesh() {
		return mesh;
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
		return stream().map(f -> getMesh().toTriangle(f));
	}

	@Override
	public Stream<Triple<IPoint, IPoint, IPoint>> streamTriples() {
		return mesh.streamFaces().map(f -> faceToTriple(f));
	}

	@Override
	public Stream<IPoint> streamPoints() {
		return mesh.streamPoints();
	}

	@Override
	public void remove(IPoint point) {
		Optional<F> optFace = locateFace(point);
		if(optFace.isPresent()) {
			F face = optFace.get();
			for(V vertex : getMesh().getVertexIt(face)) {
				if(getMesh().getPoint(vertex).equals(point)) {
					remove(vertex);
					break;
				}
			}
		}
	}

	public Collection<VTriangle> getTriangles() {
		return stream().map(face -> faceToTriangle(face)).collect(Collectors.toSet());
	}

	private Triple<IPoint, IPoint, IPoint> faceToTriple(final F face) {
		List<IPoint> points = mesh.getPoints(face);
		assert points.size() == 3;
		IPoint p1 = points.get(0);
		IPoint p2 = points.get(1);
		IPoint p3 = points.get(2);
		return Triple.of(p1, p2, p3);
	}

	private VTriangle faceToTriangle(final F face) {
		List<V> points = mesh.getEdges(face).stream().map(edge -> mesh.getVertex(edge)).collect(Collectors.toList());
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
	@Override
	public boolean isIllegal(@NotNull final E edge, @NotNull final V p) {
		// TODO: duplicated code
		if(/*!isVirtualVertex(p) && */!mesh.isAtBoundary(edge) && illegalPredicate.test(edge)) {
			V v1 = getMesh().getVertex(edge);
			V v2 = getMesh().getTwinVertex(edge);

			/*if(isVirtualVertex(v1)) {
				E e = getMesh().getNext(getMesh().getTwin(edge));
				return !isVirtualEdge(e) && isLeftOf(p.getX(), p.getY(), e);
			} else if(isVirtualVertex(v2)) {
				E e = getMesh().getPrev(getMesh().getTwin(edge));
				return !isVirtualEdge(e) && isLeftOf(p.getX(), p.getY(), e);
			} else {*/
				return isDelaunayIllegal(edge, p);
			//}
		}

		return false;
		//return isIllegal(edge, p, mesh);
	}

	@Override
	public boolean isIllegal(@NotNull final E edge, @NotNull final V p, final double eps) {
		if(/*!isVirtualVertex(p) && */!mesh.isAtBoundary(edge) && illegalPredicate.test(edge)) {
			// special case for infinity vertices for which the arc of their circumcircle becomes a line!
			//V v1 = getMesh().getVertex(edge);
			//V v2 = getMesh().getTwinVertex(edge);

			/*if(isVirtualVertex(v1)) {
				return isLeftOf(p.getX(), p.getY(), getMesh().getNext(getMesh().getTwin(edge)));
			} else if(isVirtualVertex(v2)) {
				return isLeftOf(p.getX(), p.getY(), getMesh().getPrev(getMesh().getTwin(edge)));
			} else {
			*/
				return isDelaunayIllegal(edge, p, eps);
			//}
		}

		return false;
		//return isIllegal(edge, p, mesh);
	}

	/*public static <P extends IPoint, V extends  IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> boolean isIllegal(E edge, V p, IMesh<P, V, E, F> mesh) {
		if(!mesh.isBoundary(mesh.getTwinFace(edge))) {
			//V p = mesh.getVertex(mesh.getNext(edge));
			E t0 = mesh.getTwin(edge);
			E t1 = mesh.getNext(t0);
			E t2 = mesh.getNext(t1);

			V x = mesh.getVertex(t0);
			V y = mesh.getVertex(t1);
			V z = mesh.getVertex(t2);

			//return Utils.angle3D(x, y, z) + Utils.angle3D(x, p, z) > Math.PI;

			//return Utils.isInCircumscribedCycle(x, y, z, p);
			if (Utils.ccw(x,y,z) > 0
					t.dest().rightOf(e) && v.isInCircle(e.orig(), t.dest(), e.dest())) {
			log.info(Utils.ccw(x,y,z) > 0);
			return Utils.isInsideCircle(x, y, z, p);
		}
		return false;
	}*/

	/*public static <P extends IPoint, CE, CF, V extends  IVertex, E extends IHalfEdge, F extends IFace> boolean isIllegal(E edge, V p, IMesh<V, E, F> mesh) {
		if(!mesh.isBoundary(mesh.getTwinFace(edge))) {
			//assert mesh.getVertex(mesh.getNext(edge)).equals(p);
			//V p = mesh.getVertex(mesh.getNext(edge));
			E t0 = mesh.getTwin(edge);
			E t1 = mesh.getNext(t0);
			E t2 = mesh.getNext(t1);

			V x = mesh.getVertex(t0);
			V y = mesh.getVertex(t1);
			V z = mesh.getVertex(t2);

			//return Utils.angle3D(x, y, z) + Utils.angle3D(x, p, z) > Math.PI;

			//return Utils.isInCircumscribedCycle(x, y, z, p);
			//if(Utils.ccw(z,x,y) > 0) {
				return GeometryUtils.isInsideCircle(z, x, y, p);
			//}
			//else {
			//	return Utils.isInsideCircle(x, z, y, p);
			//}
		}
		return false;
	}*/

	/*public static <P extends IPoint> boolean isIllegalEdge(final E edge){
		P p = edge.getNext().getEnd();

		if(!edge.isAtBoundary() && !edge.getTwin().isAtBoundary()) {
			P x = edge.getTwin().getEnd();
			P y = edge.getTwin().getNext().getEnd();
			P z = edge.getTwin().getNext().getNext().getEnd();
			VTriangle triangle = new VTriangle(new VPoint(x.getX(), x.getY()), new VPoint(y.getX(), y.getY()), new VPoint(z.getX(), z.getY()));
			return triangle.isInCircumscribedCycle(p);
		}
		return false;
	}*/

	@Override
	public void legalizeNonRecursive(@NotNull final E edge, final V p) {
		int flips = 0;
		int its = 0;
		//if(isIllegal(edge, p)) {

			// this should be the same afterwards
			//E halfEdge = getMesh().getNext(edge);

			IMesh<V, E, F> mesh = getMesh();
			E startEdge = mesh.getPrev(edge);
			E endEdge = mesh.getTwin(getMesh().getPrev(startEdge));
			E currentEdge = mesh.getPrev(edge);

			// flipp
			//c.prev.twin

			while(currentEdge != endEdge) {
				while (isIllegal(mesh.getNext(currentEdge), p)) {
					flip(mesh.getNext(currentEdge));
					flips++;
					its++;
				}
				its++;

				currentEdge = mesh.getTwin(mesh.getPrev(currentEdge));
			}

			//log.info("#flips = " + flips);
			//log.info("#its = " + its);
		//}
	}

	/*@Override
	/*@Override
	public void legalizeNonRecursive(@NotNull final E edge, final V p) {
		boolean found = false;
		do {
			found = false;
			for(E e : getMesh().getEdges()) {
				if(isIllegal(e)) {
					flip(e);
					found = true;
				}
			}
		} while (found);
	}*/

	@Override
	public void flipEdgeEvent(final F f1, final F f2) {
		pointLocator.postFlipEdgeEvent(f1, f2);
		for(ITriEventListener<V, E, F> triEventListener : triEventListeners) {
			triEventListener.postFlipEdgeEvent(f1, f2);
		}
	}

	@Override
	public void splitTriangleEvent(final F original, final F f1, F f2, F f3, V v) {
		pointLocator.postSplitTriangleEvent(original, f1, f2, f3,v );
		for(ITriEventListener<V, E, F> triEventListener : triEventListeners) {
			triEventListener.postSplitTriangleEvent(original, f1, f2, f3, v);
		}
	}

	@Override
	public void splitEdgeEvent(E originalEdge, F original, F f1, F f2, V v) {
		pointLocator.postSplitHalfEdgeEvent(originalEdge, original, f1, f2,v );
		for(ITriEventListener<V, E, F> triEventListener : triEventListeners) {
			triEventListener.postSplitHalfEdgeEvent(originalEdge, original, f1, f2, v);
		}
	}

	@Override
	public void insertEvent(@NotNull final E halfEdge) {
		pointLocator.postInsertEvent(getMesh().getVertex(halfEdge));
		for(ITriEventListener<V, E, F> triEventListener : triEventListeners) {
			triEventListener.postInsertEvent(getMesh().getVertex(halfEdge));
		}
	}

	@Override
	public Iterator<F> iterator() {
		return new FaceIterator(mesh);
	}

	public Stream<F> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	@Override
	public IncrementalTriangulation<V, E, F> clone() {
		try {
			IncrementalTriangulation<V, E, F> clone = (IncrementalTriangulation<V, E, F>)super.clone();
			clone.mesh = mesh.clone();

			List<V> cVirtualVertices = new ArrayList<>();
			for(V v : virtualVertices) {
				for(V cV : clone.mesh.getVertices()) {
					if(v.getPoint().equals(cV.getPoint())) {
						cVirtualVertices.add(cV);
						break;
					}
				}
			}

			assert cVirtualVertices.size() == virtualVertices.size();
			clone.virtualVertices = cVirtualVertices;

			/**
			 * The point locator is not cloned but reconstructed. Cloning the Delaunay-Hierarchy or the Delaunay-Tree seems impossible with
			 * respect to the performance. However, the reconstruction is also expensive O(n * log(n)) where n is the number of vertices.
			 */
			clone.setPointLocator(pointLocator.getType());

			return clone;

		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.getMessage());
		}
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
				IPointLocator.Type.DELAUNAY_HIERARCHY,
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
                IPointLocator.Type.DELAUNAY_HIERARCHY,
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
