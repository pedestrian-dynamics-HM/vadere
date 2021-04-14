package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.ITriEventListener;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.IRefiner;
import org.vadere.meshing.utils.io.tex.TexGraphGenerator;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.*;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>Triangulation creator: This class is realises an algorithm which refineSimplex2D a given triangulation
 * (which might be empty), by recursively splitting existing triangles (starting with the super triangle if
 * the triangulation is empty) into parts. In each step for each triangle the longest edge is split which generates
 * two new triangles (four for one edge which is part of two triangles). While splitting we generate the Sierpinski
 * Space Filling Curve (SFC) which gives an order of the generated triangles such that geometrically close triangles
 * will be close in the SFC which is therefore memory efficient. The data structure of the SFC {@link GenSpaceFillingCurve}
 * is a linked list of nodes {@link SFCNode}. Each {@link SFCNode} contains a half-edge {@link E} and a direction. The half-edge
 * is the edge of the triangle which is part of the curve and the direction tells us if the curves is in the direction of the
 * half-edge or in the reverse direction. After each split this direction changes to the opposite. After the refinement is finished
 * the mesh is updated based on the SFC i.e. vertices, edges, points and faces are re-arranged to get a cache friendly and memory
 * efficient data arrangement.</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class GenUniformRefinementTriangulatorSFC<V extends IVertex, E extends IHalfEdge, F extends IFace> implements IRefiner<V, E, F>, ITriEventListener<V, E, F> {

	private static final Logger logger = Logger.getLogger(GenUniformRefinementTriangulatorSFC.class);

	/**
	 * A collection of obstacle shapes i.e. areas defining the holes of the triangulation.
	 * This can be empty since the holes are also defined by the distance function. However,
	 * the refinement can maybe stop earlier if this information is provided.
	 */
	private final Collection<? extends VShape> boundary;

	/**
	 * The bounding box containing the whole triangulation.
	 */
	private final VRectangle bbox;

	/**
	 * The relative edge length function. If a uniform triangulation should be computed set
	 * lenFunc equlas to edge -> 1.0.
	 */
	private final IEdgeLengthFunction lenFunc;

	/**
	 * The triangulation which will be constructed.
	 */
	private IIncrementalTriangulation<V, E, F> triangulation;

	/**
	 * The mesh supplier to construct an empty mesh which containing the data (points, vertices, edges, faces).
	 */
	private final IMeshSupplier<V, E, F> meshSupplier;

	private final Map<V, VLine> projections;

	private final Collection<E> constrains;

	private final Collection<IPoint> fixPoints;

	/**
	 * The set of inserted points.
	 */
	private Set<IPoint> points;

	/**
	 * The candidates which will be refined in the next iteration.
	 */
	private ArrayList<SFCNode<V, E, F>> candidates;

	/**
	 * A distance function which has to be negative at positions which should be triangulated
	 * and positive outside.
	 */
	private final IDistanceFunction distFunc;

	/**
	 * Indicates if the refinement has finished.
	 */
	private boolean finished;

	/**
	 * A random number generator.
	 */
	private final Random random = new Random(0);

	/**
	 * The space filling curve.
	 */
	private final GenSpaceFillingCurve<V, E, F> sfc;

	/**
	 * The mesh which containing the data (points, vertices, edges, faces).
	 */
	private final IMesh<V, E, F> mesh;

	private boolean initialized;

	private boolean refinementFinished;

	private Collection<V> insertedFixPoints;

	private double smallestEdgeLength;

	private final double minEdgeLen;

	/**
	 * <p>The default constructor.</p>
	 *  @param meshSupplier          a {@link IMeshSupplier} required to generate a new and empty mesh.
	 * @param bound                 the bounding box containing all boundaries and the topography with respect to the distance function distFunc
	 * @param boundary              the boundaries e.g. obstacles
	 * @param lenFunc               an edge length function
	 * @param distFunc              a signed distance function
	 * @param fixPoints             a collection of fix points
	 */
	public GenUniformRefinementTriangulatorSFC(
			final IMeshSupplier<V, E, F> meshSupplier,
			final VRectangle bound,
			final Collection<? extends VShape> boundary,
			final IEdgeLengthFunction lenFunc,
			final IDistanceFunction distFunc,
			final Collection<IPoint> fixPoints) {

		this(meshSupplier, bound, boundary, lenFunc, Double.POSITIVE_INFINITY, distFunc, fixPoints);
	}

	public GenUniformRefinementTriangulatorSFC(
			final IMeshSupplier<V, E, F> meshSupplier,
			final VRectangle bound,
			final Collection<? extends VShape> boundary,
			final IEdgeLengthFunction lenFunc,
			final double h0,
			final IDistanceFunction distFunc,
			final Collection<IPoint> fixPoints) {
		this.fixPoints = fixPoints;
		this.smallestEdgeLength = Double.POSITIVE_INFINITY;
		this.meshSupplier = meshSupplier;
		this.initialized = false;
		this.refinementFinished = false;
		this.distFunc = distFunc;
		this.boundary = boundary;
		this.lenFunc = lenFunc;
		this.bbox = bound;
		this.points = new HashSet<>();
		this.candidates = new ArrayList<>();
		this.sfc = new GenSpaceFillingCurve<>();
		this.mesh = meshSupplier.get();
		this.insertedFixPoints = new ArrayList<>();
		this.projections = new HashMap<>();
		this.constrains = new ArrayList<>();
		this.minEdgeLen = h0;
	}

	public GenUniformRefinementTriangulatorSFC(
			final IMeshSupplier<V, E, F> meshSupplier,
			final VRectangle bound,
			final Collection<? extends VShape> boundary,
			final IEdgeLengthFunction lenFunc,
			final IDistanceFunction distFunc) {
		this(meshSupplier, bound, boundary, lenFunc, distFunc, Collections.EMPTY_LIST);
	}

	public GenUniformRefinementTriangulatorSFC(
			final IMeshSupplier<V, E, F> meshSupplier,
			final VRectangle bound,
			final IEdgeLengthFunction lenFunc,
			final IDistanceFunction distFunc,
			final Collection<IPoint> fixPoints) {
		this(meshSupplier, bound, new ArrayList<>(), lenFunc, distFunc);
	}

	public GenUniformRefinementTriangulatorSFC(
			final IMeshSupplier<V, E, F> meshSupplier,
			final VRectangle bound,
			final IEdgeLengthFunction lenFunc,
			final IDistanceFunction distFunc) {
		this(meshSupplier, bound, new ArrayList<>(), lenFunc, distFunc);
	}

	/**
	 * <p>Constructs the starting point of this algorithm:
	 * A triangulation containing 2 triangles which are the result
	 * of splitting a square which contains the bounding box.</p>
	 *
	 * @return a triangulation consisting of two triangles containing the bounding box
	 */
    public IIncrementalTriangulation<V, E, F> init() {
	    initialized = true;
    	double xMin = bbox.getMinX();
	    double yMin = bbox.getMinY();

	    double xMax = bbox.getMaxX();
	    double yMax = bbox.getMaxY();

	    double max = Math.max(xMax-xMin, yMax-yMin);

	    V p0 = mesh.insertVertex(xMin, yMin);
	    V p1 = mesh.insertVertex(xMin+max, yMin);
	    V p2 = mesh.insertVertex(xMin, yMin+max);
	    V p3 = mesh.insertVertex(xMin+max, yMin+max);

	    // counter clockwise!
	    F square = mesh.createFace(p0, p1, p3, p2);
	    F tri = mesh.createFace();

	    // start divide the square into 2 triangles
	    E edge = mesh.createEdge(p1);
	    E twin = mesh.createEdge(p2);

	    mesh.setTwin(edge, twin);

	    E start = mesh.getEdge(p2);
	    if(mesh.isBoundary(start)) {
		    start = mesh.getPrev(mesh.getTwin(start));
	    }

	    E next = mesh.getNext(start);
	    E prev = mesh.getPrev(start);
	    E nnext = mesh.getNext(next);

	    mesh.setPrev(edge, start);
	    mesh.setNext(edge, prev);

	    mesh.setNext(twin, next);
	    mesh.setPrev(twin, nnext);

	    mesh.setFace(edge, square);
	    mesh.setFace(twin, tri);
	    mesh.setFace(mesh.getNext(twin), tri);
	    mesh.setFace(mesh.getPrev(twin), tri);

	    F borderFace = mesh.getTwinFace(mesh.getEdge(square));
	    mesh.setEdge(borderFace, mesh.getTwin(start));

	    mesh.setEdge(tri, twin);
	    mesh.setEdge(square, edge);
		// end divide the square into 2 triangles

	    E halfEdge = getLongestEdge(mesh.getFace());
	    SFCNode<V, E, F> node1 = new SFCNode<>(halfEdge, SFCDirection.FORWARD);
	    SFCNode<V, E, F> node2 = new SFCNode<>(getMesh().getTwin(halfEdge), SFCDirection.FORWARD);

	    candidates.add(node1);
	    candidates.add(node2);
	    sfc.insertFirst(node1);
	    sfc.insertNext(node2, node1);
	    triangulation = mesh.toTriangulation(IPointLocator.Type.JUMP_AND_WALK);
	    return triangulation;
    }

    @Override
    public IMesh<V, E, F> getMesh() {
    	return mesh;
    }

	/**
	 * <p>Applies one iteration in the construction of the space filling curve i.e. the next level will be
	 * reached. If there are n candidates at most 2*n new candidates will be created:
	 *
	 * <ol>
	 *     <li>update the SFC:  if the node k is a candidate which is not complete the node will be replaces in the
	 * 	                        SFC by two new nodes. The order of these two nodes and the directions depends on the
	 * 	                        direction of k.</li>
	 * 	    <li>
	 * 	        refineSimplex2D edges:   After the SFC is updated we can refineSimplex2D edges. This won't destroy the edges of the nodes of
	 * 	                        the SFC.
	 * 	    </li>
	 * </ol>
	 *
	 * We have to do step (1) before step (2) since an edge split, splits two half-edges which are not necessarily
	 * neighbours in the SFC. Therefore, the split order is not equals to the order of the SFC.
	 * </p>
	 *
	 * @param refinePredicate the predicate which determines if the edge will be refined further.
	 */
	private void nextSFCLevel(@NotNull final Predicate<E> refinePredicate) {
		//System.out.println(curveToTikz());
		//List<SFCNode<P, V, E ,F>> candidates = sfc.asList();
		ArrayList<SFCNode<V, E ,F>> newCandidates = new ArrayList<>(candidates.size() * 2);
		//Map<E, SFCNode<P, V, E, F>> newEdgeToNode = new HashMap<>();

		ArrayList<E> toRefineEdges = new ArrayList<>();
		boolean tFinished = true;

		// 1. update CFS before refinement!
		for(SFCNode<V, E ,F> node : candidates) {
			E edge = node.getEdge();

			if(refinePredicate.test(edge)) {
				toRefineEdges.add(edge);
				tFinished = false;
				SFCDirection dir = node.getDirection();
				E t1 = getMesh().getNext(edge);
				E t2 = getMesh().getPrev(edge);

				SFCNode<V, E ,F> element1 = new SFCNode<>(t1, dir.next());
				SFCNode<V, E ,F> element2 = new SFCNode<>(t2, dir.next());

				if(dir == SFCDirection.FORWARD) {
					newCandidates.add(element2);
					newCandidates.add(element1);
					sfc.replace(element2, element1, node);
				}
				else {
					newCandidates.add(element1);
					newCandidates.add(element2);
					sfc.replace(element1, element2, node);
				}

				//newEdgeToNode.put(t1, element1);
				//newEdgeToNode.put(t2, element2);
			}
			else {
				//newEdgeToNode.put(edge, node);
			}
		}

		// 2. refineSimplex2D
		for(E edge : toRefineEdges) {
			// to avoid duplicated splits
			if(validEdge(edge)) {
				refine(edge);
			}
		}
		refinementFinished = tFinished;
		candidates = newCandidates;
	}

	private V splitEdge(@NotNull final E edge) {
		if(!getMesh().isBoundary(edge)) {
			SFCNode<V, E, F> node = sfc.getNode(edge);
			SFCDirection dir = node.getDirection();
			E t1 = getMesh().getNext(edge);
			E t2 = getMesh().getPrev(edge);

			SFCNode<V, E ,F> element1 = new SFCNode<>(t1, dir.next());
			SFCNode<V, E ,F> element2 = new SFCNode<>(t2, dir.next());

			if(dir == SFCDirection.FORWARD) {
				sfc.replace(element2, element1, node);
			}
			else {
				sfc.replace(element1, element2, node);
			}
		}


		E twin = getMesh().getTwin(edge);
		if(!getMesh().isBoundary(twin)) {
			SFCNode<V, E, F> node = sfc.getNode(twin);
			SFCDirection dir = node.getDirection();
			E t1 = getMesh().getNext(edge);
			E t2 = getMesh().getPrev(edge);

			SFCNode<V, E ,F> element1 = new SFCNode<>(t1, dir.next());
			SFCNode<V, E ,F> element2 = new SFCNode<>(t2, dir.next());

			if(dir == SFCDirection.FORWARD) {
				sfc.replace(element2, element1, node);
			}
			else {
				sfc.replace(element1, element2, node);
			}
		}

		return refine(edge);
	}

	private void nextSFCLevel() {
		nextSFCLevel(edge -> !isCompleted(edge) && triangulation.isLongestEdge(edge));
	}

	private void nextSFCLevel(double ran) {
		nextSFCLevel(edge -> (random.nextDouble() < ran) && triangulation.isLongestEdge(edge));
	}

	/**
	 * <p>There are two half-edges for each edge but we want to have only one half-edge for each edge
	 * for a split. This method helps do filter one of those edges.</p>
	 *
	 * @param edge the half-edge
	 * @return true if this half-edge is valid (which means its twin is invalid), false otherwise
	 */
	private boolean validEdge(@NotNull E edge) {
		if (getMesh().isAtBoundary(edge)) {
			return true;
		}
		IPoint p1 = getMesh().getPoint(getMesh().getPrev(edge));
		IPoint p2 = getMesh().getPoint(edge);
		return (p1.getX() > p2.getX() || (p1.getX() == p2.getX() && p1.getY() > p2.getY()));
	}

	/**
	 * <p>Applies the next step of the algorithm i.e. for the first call it initializes the algorithm,
	 * all calls afterwards until the refinement has finished the next SFC-level is constructed, if
	 * the refinement has finished the finish part is executed.</p>
	 */
	@Override
	public void refine() {
		if(!initialized) {
			init();
			initialized = true;
		}

		if(!refinementFinished) {
			nextSFCLevel();
		}
		else if(!finished) {
			finish();
		}
	}

	/**
	 * <p>Returns true if the refinement is finished.</p>
	 *
	 * @return true if the refinement is finished, otherwise false
	 */
    public boolean isFinished() {
        return finished;
    }

	/**
	 * <p>Returns the triangulation of this refinement.</p>
	 *
	 * @return the triangulation of this refinement
	 */
	@Override
	public IIncrementalTriangulation<V, E, F> getTriangulation() {
		if(triangulation == null) {
			init();
		}
		return triangulation;
	}

	/**
	 * <p>Starts the refinement.</p>
	 *
	 * @return returns the refined triangulation
	 */
	public IIncrementalTriangulation<V, E, F> generate() {
		return generate(true);
	}

	@Override
	public IIncrementalTriangulation<V, E, F> generate(boolean finalize) {
		if(!isFinished()) {
			logger.info("start triangulation generation");
			init();

			while (!isFinished()) {
				refine();
			}

			if(finalize) {
				finish();
			}
			logger.info("end triangulation generation");
		}
		return triangulation;
	}

	/**
	 * <p>Refines an edge, i.e. splits the edge at its midpoint if the midpoint is not already contained in
	 * the list of points. The triangulation notifies its listeners about this event.</p>
	 *
	 * @param edge the edge which will be refined / split
	 * @return returns the vertex inserted
	 */
	private V refine(final E edge) {
		//TODO: magic number 0.01
		VLine line = getMesh().toLine(edge);
		IPoint midPoint = line.midPoint(random.nextDouble() * 0.01);

		V v = getMesh().createVertex(midPoint.getX(), midPoint.getY());
		IPoint p = getMesh().getPoint(v);

		if(!points.contains(p)) {
			points.add(p);
			E newEdge = triangulation.getAnyEdge(triangulation.splitEdge(v, edge, false));
			triangulation.insertEvent(newEdge);
			smallestEdgeLength = Math.min(smallestEdgeLength, line.length() / 2.0);
		}
		else {
			throw new IllegalStateException(p + " point already exist.");
		}
		return v;
	}

	/**
	 * <p>Called after the splitting is completed. This method creates all holes, removes triangles outside the bounding box,
	 * inserts fix points and re-arranges the mesh data structure such that it is cache friendly.</p>
	 */
	public void finish() {
		if(!finished) {
			synchronized (getMesh()) {
				//nextSFCLevel(0.2);
				triangulation.addTriEventListener(this);
				finished = true;
				// TODO: adjust sierpinsky order, idea: construct a tree -> locate the face using the tree -> replace the face by the three new faces
				insertFixPoints(fixPoints);
				triangulation.finish();

				// the following calls are quite expensive
				establishConstrains();
				List<F> sierpinksyFaceOrder = sfc.asList().stream().map(node -> getMesh().getFace(node.getEdge())).collect(Collectors.toList());
				shrinkBorder();
				createHoles();
				adjustBoundaryVertexEdges();
				//triangulation.smoothBorder();

				sierpinksyFaceOrder.removeIf(face -> getMesh().isDestroyed(face) || getMesh().isHole(face));
				List<F> holes = getMesh().streamHoles().collect(Collectors.toList());
				logger.info("#holes:" + holes.size());
				sierpinksyFaceOrder.addAll(holes);
				logger.info("#sier-faces:" + sierpinksyFaceOrder.size() + ", #vertices" + getMesh().getNumberOfVertices());

				getMesh().arrangeMemory(sierpinksyFaceOrder);
				triangulation.removeTriEventListener(this);
				getMesh().garbageCollection();
				logger.info("#sier-faces:" + sierpinksyFaceOrder.size() + ", #faces" + getMesh().getNumberOfFaces());
			}
		}
    }

	private void insertFixPoints(@NotNull final Collection<IPoint> fixPoints) {
		//int count = 1;
		for(IPoint point : fixPoints) {
			V vertex = getMesh().createVertex(point);
			//assert getMesh().getFaces().stream().noneMatch(f -> getNode(f) == null) : "count " + count;
			V insertedVertex = getMesh().getVertex(getTriangulation().insertVertex(vertex, false));
			getMesh().setBooleanData(vertex, "fixPoint", true);
			//getMesh().getFaces().stream().filter(f -> getNode(f) == null).forEach(f -> System.out.println(f));
			//assert getMesh().getFaces().stream().noneMatch(f -> getNode(f) == null) : "count " + count;
			//count++;
		}
	}

	@Override
	public Collection<V> getFixPoints() {
		return getMesh().streamVertices().filter(v -> getMesh().getBooleanData(v, "fixPoint")).collect(Collectors.toList());
	}


	/**
	 * <p>Insert fix points i.e. point that has to be in the triangulation and which will not be
	 * moved. If there is a point p very close to the fix, instead of
	 * inserting the fix point, the point p will be moved to the position of the fix point
	 * and will be marked as fix point.</p>
	 *
	 * @param fixPoints a collection of fix points.
	 */
	/*private Map<V, VPoint> insertFixPoints(@NotNull final Collection<VPoint> fixPoints) {
		Map<V, VPoint> fixPointRelation = new HashMap<>();
		for(VPoint fixPoint : fixPoints) {
			Optional<F> optFace = triangulation.locate(fixPoint.getX(), fixPoint.getY());
			if(optFace.isPresent()) {
				assert triangulation.contains(fixPoint.getX(), fixPoint.getY(), optFace.get());
				F face = optFace.get();
				V closestPoint = null;
				double distance = Double.MAX_VALUE;
				for(V v : getMesh().getVertexIt(face)) {
					P q = getMesh().getPoint(v);
					double tmpDistance = Math.abs(distFunc.apply(q)- distFunc.apply(fixPoint));
					if(!fixPointRelation.containsKey(v) && (closestPoint == null ||  tmpDistance < distance)) {
						closestPoint = v;
						distance = tmpDistance;
					}
				}
				assert closestPoint != null;

				// we have a problem here!
				if(closestPoint == null) {
					throw new IllegalArgumentException("fix points are to close together: use another set of fix points or a finer mesh");
					//V vertex = splitEdge(getLongestEdge(face));
					//fixPointRelation.put(vertex, fixPoint);
				} else {
					fixPointRelation.put(closestPoint, fixPoint);
				}
			}
		}
		return fixPointRelation;
    }*/

	/**
	 * <p>Returns the longest edge of a face.</p>
	 *
	 * @param face the face
	 * @return  the lonngest edge of a face.
	 */
	private E getLongestEdge(F face) {
	    return getMesh().streamEdges(face).reduce((e1, e2) -> getMesh().toLine(e1).length() > getMesh().toLine(e2).length() ? e1 : e2).get();
    }

	/**
	 * <p>Shrinks the border such that there are no more triangles outside the boundary i.e. where the distance is positive.
	 * Note the border is part of the whole boundary which is defined by the border and the holes.</p>
	 */
	private void shrinkBorder() {
		Predicate<F> removePredicate = face -> distFunc.apply(triangulation.getMesh().toMidpoint(face)) > 0;
		triangulation.shrinkBorder(removePredicate, true, false);
	}

	@Override
	public Map<V, VLine> getProjections() {
		return projections;
	}

	@Override
	public Collection<E> getConstrains() {
		return constrains;
	}

	/**
	 * <p>Creates holes everywhere where the distance function is positive. Neighbouring holes will be merged.</p>
	 */
	/*private void createHoles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> distFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
			}
		}
	}*/



	private void establishConstrains() {
		List<VLine> lines = boundary.stream().flatMap(shape -> shape.lines().stream()).collect(Collectors.toList());
		//GenConstrainedDelaunayTriangulator<V, E, F> cdt = new GenConstrainedDelaunayTriangulator<>(getTriangulation(), lines, true);
		//GenConstrainSplitter<V, E, F> cdt = new GenConstrainSplitter<>(getTriangulation(), lines, minEdgeLen / 2);

		GenConstrainSplitter<V, E, F> cdt = new GenConstrainSplitter<>(getTriangulation(), lines, GeometryUtils.DOUBLE_EPS, sfc);
		cdt.generate(false);
		getTriangulation().setCanIllegalPredicate(e -> true);
		projections.putAll(cdt.getProjections());
		constrains.addAll(cdt.getConstrains());

	}

	private void createHoles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> distFunc.apply(triangulation.getMesh().toMidpoint(f)) > 0, true, false);
			}
		}
	}

	private void adjustBoundaryVertexEdges() {
		for(F hole : getMesh().getHoles()) {
			for(V v : getMesh().getVertexIt(hole)) {
				triangulation.adjustVertex(v);
			}
		}

		for(V v : getMesh().getVertexIt(getMesh().getBorder())) {
			triangulation.adjustVertex(v);
		}
	}

	/*private void createHoles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> intersect(triangulation.getMesh().toTriangle(f)), true);
			}
		}
	}*/

	private boolean intersect(@NotNull final VPoint p1, @NotNull final VPoint p2) {
		double dist1 = distFunc.apply(p1);
		double dist2 = distFunc.apply(p2);
		double dist = GeometryUtils.distance(p1.x, p1.y, p2.x, p2.y);

		if(dist+dist1+dist1 > 0){
			VPoint dir = p2.subtract(p1).norm();
			VPoint start = p1.add(dir.setMagnitude(-dist1));
			VPoint end = p2.subtract(dir.setMagnitude(-dist2));
			double len = start.distance(end);
			for(double dx = smallestEdgeLength * 0.2; dx < len; dx += smallestEdgeLength) {
				VPoint dxy = dir.setMagnitude(dx);

				if(distFunc.apply(start.add(dxy)) > smallestEdgeLength * 0.2) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean intersect(@NotNull final VTriangle triangle) {
		if(distFunc.apply(triangle.midPoint()) > 0) {
			return true;
		}

		double d1 = distFunc.apply(triangle.p1);
		double d2 = distFunc.apply(triangle.p2);
		double d3 = distFunc.apply(triangle.p3);

		if((d1 > smallestEdgeLength && d2 > smallestEdgeLength) || (d1 > smallestEdgeLength && d3 > smallestEdgeLength) || (d2 > smallestEdgeLength && d3 > smallestEdgeLength)) {
			return true;
		}

		/*boolean i1 = d1 > smallestEdgeLength && d2 > smallestEdgeLength && intersect(triangle.p1, triangle.p2);
		boolean i2 = d2 > smallestEdgeLength && d3 > smallestEdgeLength && intersect(triangle.p2, triangle.p3);
		boolean i3 = d3 > smallestEdgeLength && d1 > smallestEdgeLength && intersect(triangle.p3, triangle.p1);
		return (i1 && i2) || (i1 && i3) || (i2 && i3);*/
		return false;
	}

	/**
	 * <p>Removes acute angles at the boundary of holes.</p>
	 */
	private void smoothHoles() {
		for(F hole : getMesh().getHoles()) {
			for(E edge : getMesh().getEdges(hole)) {
				if(getMesh().isBoundary(edge)) {

					VPoint p = getMesh().toPoint(edge);
					VPoint q = getMesh().toPoint(getMesh().getNext(edge));
					VPoint r = getMesh().toPoint(getMesh().getPrev(edge));

					if(GeometryUtils.isCCW(r, p, q)) {
						double angle = GeometryUtils.angle(r, p, q);
						if(angle < 0.5*Math.PI) {
							triangulation.createFaceAtBoundary(edge);
						}
					}
				}
			}
		}
	}

	/**
	 * <p>Tests if a specific edge is complete i.e. it should not be split into two edges.</p>
	 *
	 * @param edge the half-edge representing the edge of the mesh.
	 * @return true, if the edge should not be split, false otherwise
	 */
	private boolean isCompleted(@NotNull E edge) {
		if(getMesh().isBoundary(edge)){
			edge = getMesh().getTwin(edge);
		}

		return isSmallEnough(edge) /* || isOutside(edge) || isEdgeOutsideBBox(edge) || isEdgeInsideHole(edge);*/;
	}

	/**
	 * <p>Tests if a specific edge is small enough.</p>
	 *
	 * @param edge the half-edge representing the edge
	 * @return true, if the edge is small enough, false otherwise
	 */
	private boolean isSmallEnough(@NotNull final E edge) {
		VLine line = getMesh().toLine(edge);
		LinkedList<VLine> lines = new LinkedList<>();
		lines.addFirst(line);
		while (!lines.isEmpty()) {
			VLine l = lines.poll();

			if(line.length() > lenFunc.apply(l.midPoint())) {
				return false;
			}

			if(l.length() > minEdgeLen) {
				lines.addLast(new VLine(l.getVPoint1(), l.midPoint()));
				lines.addLast(new VLine(l.getVPoint2(), l.midPoint()));
			}
		}

		return true;
	}

	/**
	 * <p>Returns true if all neighbouring faces (one or two) of the edge represented by the half-edge
	 * are not contained in the bounding box. In this case the refinement can stop.</p>
	 *
	 * @param edge the half-edge
	 * @return true if all neighbouring faces (one or two) of the edge are not contained in the bounding box
	 */
	private boolean isNeighbouringFacesContainedInBoundingBox(@NotNull final E edge) {
		F face = getMesh().getFace(edge);
		F twin = getMesh().getTwinFace(edge);

		VTriangle triangle = getMesh().toTriangle(face);
		return (!triangle.intersectsRectangleLine(bbox) && (getMesh().isBoundary(twin) || !getMesh().toTriangle(twin).intersectsRectangleLine(bbox)));
	}

	/**
	 * <p>Returns true if all neighbouring faces (one or two) of the edge represented by the half-edge
	 * are not contained in an obstacle shape. In this case the refinement can stop.</p>
	 *
	 * @param edge the half-edge
	 * @return true if all neighbouring faces (one or two) of the edge are not contained an obstacle face
	 */
	private boolean isNeighbouringFacesContainedInObstacle(@NotNull final E edge) {
		if(boundary.isEmpty()) {
			return false;
		}
		else {
			F face = getMesh().getFace(edge);
			F twin = getMesh().getTwinFace(edge);

			VTriangle triangle = getMesh().toTriangle(face);
			return boundary.stream()
					.anyMatch(shape -> shape.contains(triangle.getBounds2D()) && (getMesh().isBoundary(twin) || shape.contains(getMesh().toTriangle(twin).getBounds2D())));
		}
	}

	/*
	 * This does not work at the moment: TODO: split if edge is not small enough (also if it is not the longest edge).
	 * If the edge is not the longest edge split the as long as it is the case!
	 */
	private boolean isOutside(@NotNull E edge) {
		IPoint p1 = getMesh().getPoint(edge);
		IPoint p2 = getMesh().getPoint(getMesh().getNext(edge));
		IPoint p3 = getMesh().getPoint(getMesh().getNext(getMesh().getTwin(edge)));

		IPoint midPoint = getMesh().toLine(edge).midPoint();

		double len = Math.max(midPoint.distance(p1), midPoint.distance(p2));
		len = Math.max(len, midPoint.distance(p3));

		return distFunc.apply(midPoint) >= len;
	}

    private String curveToTikz() {
		return TexGraphGenerator.toTikz(getMesh(), sfc.asList().stream().map(node -> getMesh().getFace(node.getEdge())).collect(Collectors.toList()));
    }


	@Override
	public void postSplitTriangleEvent(F original, F f1, F f2, F f3, V v) {
		E e1 = getMesh().getEdge(f1);
		E e2 = getMesh().getEdge(f2);
		E e3 = getMesh().getEdge(f3);

		for(E edge : getMesh().getEdgeIt(f1)) {
			SFCNode<V, E, F> node = sfc.getNode(edge);
			if(node != null) {
				SFCNode<V, E ,F> element1 = new SFCNode<>(e1, node.getDirection().next());
				SFCNode<V, E ,F> element2 = new SFCNode<>(e2, node.getDirection().next());
				SFCNode<V, E ,F> element3 = new SFCNode<>(e3, node.getDirection().next());
				sfc.replace(element1, element2, element3, node);
				return;
			}
		}

		for(E edge : getMesh().getEdgeIt(f2)) {
			SFCNode<V, E, F> node = sfc.getNode(edge);
			if(node != null) {
				SFCNode<V, E ,F> element1 = new SFCNode<>(e1, node.getDirection().next());
				SFCNode<V, E ,F> element2 = new SFCNode<>(e2, node.getDirection().next());
				SFCNode<V, E ,F> element3 = new SFCNode<>(e3, node.getDirection().next());
				sfc.replace(element1, element2, element3, node);
				return;
			}
		}

		for(E edge : getMesh().getEdgeIt(f3)) {
			SFCNode<V, E, F> node = sfc.getNode(edge);
			if(node != null) {
				SFCNode<V, E ,F> element1 = new SFCNode<>(e1, node.getDirection().next());
				SFCNode<V, E ,F> element2 = new SFCNode<>(e2, node.getDirection().next());
				SFCNode<V, E ,F> element3 = new SFCNode<>(e3, node.getDirection().next());
				sfc.replace(element1, element2, element3, node);
				return;
			}
		}
	}

	@Override
	public void postSplitHalfEdgeEvent(E originalEdge, F original, F f1, F f2, V v) {
		SFCNode<V, E, F> node = getNode(f1, f2);
		SFCDirection dir = node.getDirection();

		E e1 = getMesh().getEdge(f1);
		E e2 = getMesh().getEdge(f2);

		SFCNode<V, E ,F> element1 = new SFCNode<>(e1, dir.next());
		SFCNode<V, E ,F> element2 = new SFCNode<>(e2, dir.next());

		sfc.replace(element1, element2, node);
	}

	private SFCNode<V, E ,F> getNode(F f1, F f2) {
		for(E edge : getMesh().getEdgeIt(f1)) {
			SFCNode<V, E, F> node = sfc.getNode(edge);
			if(node != null) {
				return node;
			}
		}

		for(E edge : getMesh().getEdgeIt(f2)) {
			SFCNode<V, E, F> node = sfc.getNode(edge);
			if(node != null) {
				return node;
			}
		}

		return null;
	}

	private SFCNode<V, E ,F> getNode(F f1) {
		for(E edge : getMesh().getEdgeIt(f1)) {
			SFCNode<V, E, F> node = sfc.getNode(edge);
			if(node != null) {
				return node;
			}
		}
		return null;
	}

	@Override
	public void postFlipEdgeEvent(F f1, F f2) {}

	@Override
	public void postInsertEvent(V vertex) {}
}
