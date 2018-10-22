package org.vadere.geometry.mesh.triangulation.triangulator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.geometry.Utils;
import org.vadere.geometry.IDistanceFunction;
import org.vadere.geometry.mesh.inter.*;
import org.vadere.geometry.shapes.*;
import org.vadere.geometry.tex.TexGraphGenerator;
import org.vadere.geometry.mesh.triangulation.adaptive.IEdgeLengthFunction;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>Triangulation creator: This class is realises an algorithm which refine a given triangulation
 * (which might be empty), by recursively splitting existing triangles (starting with the super triangle if
 * the triangulation is empty) into parts. In each step for each triangle the longest edge is split which generates
 * two new triangles (four for one edge which is part of two triangles). While splitting we generate the Sierpinski
 * Space Filling Curve (SFC) which gives an order of the generated triangles such that geometrically close triangles
 * will be close in the SFC which is therefore memory efficient. The data structure of the SFC {@link SpaceFillingCurve}
 * is a linked list of nodes {@link SFCNode}. Each {@link SFCNode} contains a half-edge {@link E} and a direction. The half-edge
 * is the edge of the triangle which is part of the curve and the direction tells us if the curves is in the direction of the
 * half-edge or in the reverse direction. After each split this direction changes to the opposite. After the refinement is finished
 * the mesh is updated based on the SFC i.e. vertices, edges, points and faces are re-arranged to get a cache friendly and memory
 * efficient data arrangement.</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> generic type of the point
 * @param <V> generic type of the vertex
 * @param <E> generic type of the half-edge
 * @param <F> generic type of the face
 */
public class UniformRefinementTriangulatorSFC<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulator<P, V, E, F> {

	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulatorSFC.class);

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
	private ITriangulation<P, V, E, F> triangulation;

	/**
	 * The mesh supplier to construct an empty mesh which containing the data (points, vertices, edges, faces).
	 */
	private final IMeshSupplier<P, V, E, F> meshSupplier;

	/**
	 * The set of inserted points.
	 */
	private Set<P> points;

	/**
	 * The candidates which will be refined in the next iteration.
	 */
	private ArrayList<SFCNode<P, V, E, F>> candidates;

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
	 * A user defined set of fix points.
	 */
	private final Collection<P> fixPoints;

	/**
	 * A random number generator.
	 */
	private final Random random = new Random();

	/**
	 * The space filling curve.
	 */
	private final SpaceFillingCurve<P, V, E, F> sfc;

	/**
	 * The mesh which containing the data (points, vertices, edges, faces).
	 */
	private final IMesh<P, V, E, F> mesh;

	private boolean initialized;

	private boolean refinementFinished;

	private double minEdgeLength;

	/**
	 * <p>The default constructor.</p>
	 *
     * @param meshSupplier          a {@link IMeshSupplier} required to generate a new and empty mesh.
     * @param bound                 the bounding box containing all boundaries and the topography with respect to the distance function distFunc
     * @param boundary              the boundaries e.g. obstacles
     * @param lenFunc               an edge length function
     * @param distFunc              a signed distance function
     */
	public UniformRefinementTriangulatorSFC(
			final IMeshSupplier<P, V, E, F> meshSupplier,
			final VRectangle bound,
			final Collection<? extends VShape> boundary,
			final IEdgeLengthFunction lenFunc,
			final double minEdgeLength,
			final IDistanceFunction distFunc,
			final Collection<P> fixPoints) {

		this.meshSupplier = meshSupplier;
		this.initialized = false;
		this.refinementFinished = false;
	    this.distFunc = distFunc;
		this.boundary = boundary;
		this.lenFunc = lenFunc;
		this.minEdgeLength = minEdgeLength;
		this.bbox = bound;
		this.fixPoints = fixPoints;
		this.points = new HashSet<>();
		this.candidates = new ArrayList<>();
		this.sfc = new SpaceFillingCurve<>();
		this.mesh = meshSupplier.get();
	}

	public UniformRefinementTriangulatorSFC(
			final IMeshSupplier<P, V, E, F> meshSupplier,
			final VRectangle bound,
			final IEdgeLengthFunction lenFunc,
			final double minEdgeLength,
			final IDistanceFunction distFunc,
			final Collection<P> fixPoints) {
		this(meshSupplier, bound, new ArrayList<>(), lenFunc, minEdgeLength, distFunc, fixPoints);
	}

	public UniformRefinementTriangulatorSFC(
			final IMeshSupplier<P, V, E, F> meshSupplier,
			final VRectangle bound,
			final IEdgeLengthFunction lenFunc,
			final double minEdgeLength,
			final IDistanceFunction distFunc) {
		this(meshSupplier, bound, new ArrayList<>(), lenFunc, minEdgeLength, distFunc, new ArrayList<>());
	}

	/**
	 * <p>Constructs the starting point of this algorithm:
	 * A triangulation containing 2 triangles which are the result
	 * of splitting a square which contains the bounding box.</p>
	 *
	 * @return a triangulation consisting of two triangles containing the bounding box
	 */
    public ITriangulation<P, V, E, F> init() {
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
	    SFCNode<P, V, E, F> node1 = new SFCNode<>(halfEdge, SFCDirection.FORWARD);
	    SFCNode<P, V, E, F> node2 = new SFCNode<>(getMesh().getTwin(halfEdge), SFCDirection.FORWARD);

	    candidates.add(node1);
	    candidates.add(node2);
	    sfc.insertFirst(node1);
	    sfc.insertNext(node2, node1);
	    triangulation = mesh.toTriangulation(IPointLocator.Type.JUMP_AND_WALK);
	    return triangulation;
    }

    private IMesh<P, V, E, F> getMesh() {
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
	 * 	        refine edges:   After the SFC is updated we can refine edges. This won't destroy the edges of the nodes of
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
		ArrayList<SFCNode<P, V, E ,F>> newCandidates = new ArrayList<>(candidates.size() * 2);
		//Map<E, SFCNode<P, V, E, F>> newEdgeToNode = new HashMap<>();

		ArrayList<E> toRefineEdges = new ArrayList<>();
		boolean tFinished = true;

		// 1. update CFS before refinement!
		for(SFCNode<P, V, E ,F> node : candidates) {
			E edge = node.getEdge();

			if(refinePredicate.test(edge)) {
				toRefineEdges.add(edge);
				tFinished = false;
				SFCDirection dir = node.getDirection();
				E t1 = getMesh().getNext(edge);
				E t2 = getMesh().getPrev(edge);

				SFCNode<P, V, E ,F> element1 = new SFCNode<>(t1, dir.next());
				SFCNode<P, V, E ,F> element2 = new SFCNode<>(t2, dir.next());

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

		// 2. refine
		for(E edge : toRefineEdges) {
			// to avoid duplicated splits
			if(validEdge(edge)) {
				refine(edge);
			}
		}
		refinementFinished = tFinished;
		candidates = newCandidates;
	}

	private void nextSFCLevel() {
		nextSFCLevel(edge -> !isCompleted(edge) && isLongestEdge(edge));
	}

	private void nextSFCLevel(int ran) {
		nextSFCLevel(edge -> (random.nextDouble() < ran) && isLongestEdge(edge));
	}

	/**
	 * <p>Applies the next step of the algorithm i.e. for the first call it initializes the algorithm,
	 * all calls afterwards until the refinement has finished the next SFC-level is constructed, if
	 * the refinement has finished the finish part is executed.</p>
	 */
    public void step() {
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
		P p1 = getMesh().getPoint(getMesh().getPrev(edge));
		P p2 = getMesh().getPoint(edge);
		return (p1.getX() > p2.getX() || (p1.getX() == p2.getX() && p1.getY() > p2.getY()));
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
	public ITriangulation<P, V, E, F> getTriangulation() {
		return triangulation;
	}

	/**
	 * <p>Starts the refinement.</p>
	 *
	 * @return returns the refined triangulation
	 */
	public ITriangulation<P, V, E, F> generate() {
        logger.info("start triangulation generation");
        init();

		while (!isFinished()) {
			step();
		}

		//nextSFCLevel(0.23);
        finish();
		logger.info("end triangulation generation");
		return triangulation;
	}

	/**
	 * <p>Refines an edge, i.e. splits the edge at its midpoint if the midpoint is not already contained in
	 * the list of points. The triangulation notifies its listeners about this event.</p>
	 *
	 * @param edge the edge which will be refined / split
	 */
	private void refine(final E edge) {
		IPoint midPoint = getMesh().toLine(edge).midPoint();
		P p = getMesh().createPoint(midPoint.getX(), midPoint.getY());

		if(!points.contains(p)) {
			points.add(p);
			E newEdge = triangulation.getAnyEdge(triangulation.splitEdge(p, edge, true));
			triangulation.insertEvent(newEdge);
		}
		else {
			throw new IllegalStateException(p + " point already exist.");
		}
	}

	/**
	 * <p>Called after the splitting is completed. This method creates all holes, removes triangles outside the bounding box,
	 * inserts fix points and re-arranges the mesh data structure such that it is cache friendly.</p>
	 */
	public void finish() {
		if(!finished) {
			synchronized (getMesh()) {
				finished = true;
				List<F> sierpinksyFaceOrder = sfc.asList().stream().map(node -> getMesh().getFace(node.getEdge())).collect(Collectors.toList());

				// TODO: adjust sierpinsky order, idea: construct a tree -> locate the face using the tree -> replace the face by the three new faces
				insertFixPoints(fixPoints);
				triangulation.finish();

				// the following calls are quite expensive
				shrinkBorder();
				createHoles();

				triangulation.smoothBorder();

				sierpinksyFaceOrder.removeIf(face -> getMesh().isDestroyed(face) || getMesh().isHole(face));
				List<F> holes = getMesh().streamHoles().collect(Collectors.toList());
				logger.info("#holes:" + holes.size());
				sierpinksyFaceOrder.addAll(holes);
				logger.info(sierpinksyFaceOrder.size() + ", " + getMesh().getNumberOfFaces());

				getMesh().arrangeMemory(sierpinksyFaceOrder);
				triangulation.getMesh().garbageCollection();
			}
		}
    }

	/**
	 * <p>Insert fix points i.e. point that has to be in the triangulation and which will not be
	 * moved. If there is a point p very close to the fix, instead of
	 * inserting the fix point, the point p will be moved to the position of the fix point
	 * and will be marked as fix point.</p>
	 *
	 * @param fixPoints a collection of fix points.
	 */
	private void insertFixPoints(@NotNull final Collection<P> fixPoints) {

		for(P fixPoint : fixPoints) {
			Optional<F> optFace = triangulation.locateFace(fixPoint);
			if(optFace.isPresent()) {
				V closestPoint = null;
				double distance = Double.MAX_VALUE;
				for(V v : getMesh().getVertexIt(optFace.get())) {
					P q = getMesh().getPoint(v);
					double tmpDistance = Math.abs(distFunc.apply(q)- distFunc.apply(fixPoint));
					if(closestPoint == null ||  tmpDistance < distance) {
						closestPoint = v;
						distance = tmpDistance;
					}
				}

				if(closestPoint != null && distance < minEdgeLength) {
					triangulation.replacePoint(closestPoint, fixPoint);
				}
				else {
					triangulation.insert(fixPoint);
				}
			}
		}
    }

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
	 * <p>Returns true if the full-edge of this half-edge is the longest edge of its faces.</p>
	 *
	 * @param edge the half-edge
	 * @return true if the full-edge of this half-edge is the longest edge of its faces
	 */
	private boolean isLongestEdge(E edge) {

		// the edge is part of two faces
		if(!getMesh().isAtBoundary(edge)) {
			E longestEdge1 = getMesh().streamEdges(getMesh().getFace(edge)).reduce((e1, e2) -> getMesh().toLine(e1).length() > getMesh().toLine(e2).length() ? e1 : e2).get();
			E longestEdge2 = getMesh().streamEdges(getMesh().getTwinFace(edge)).reduce((e1, e2) -> getMesh().toLine(e1).length() > getMesh().toLine(e2).length() ? e1 : e2).get();
			return getMesh().isSameLineSegment(longestEdge1, edge) && getMesh().isSameLineSegment(longestEdge2, edge);
		} // the edge is part of one face
	    else {
			if(getMesh().isBoundary(edge)) {
				edge = getMesh().getTwin(edge);
			}

			E longestEdge = getMesh().streamEdges(getMesh().getFace(edge)).reduce((e1, e2) -> getMesh().toLine(e1).length() > getMesh().toLine(e2).length() ? e1 : e2).get();
			return getMesh().isSameLineSegment(longestEdge, edge);
		}
    }

	/**
	 * <p>Shrinks the border such that there are no more triangles outside the boundary i.e. where the distance is positive.
	 * Note the border is part of the whole boundary which is defined by the border and the holes.</p>
	 */
	private void shrinkBorder() {
		Predicate<F> removePredicate = face -> distFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0;
		triangulation.shrinkBorder(removePredicate, true);
	}

	/**
	 * <p>Creates holes everywhere where the distance function is positive. Neighbouring holes will be merged.</p>
	 */
	private void createHoles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> distFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
			}
		}
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

					if(Utils.isCCW(r, p, q)) {
						double angle = Utils.angle(r, p, q);
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
		return (line.length() <= lenFunc.apply(line.midPoint()) * minEdgeLength);
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
		return (!triangle.intersects(bbox) && (getMesh().isBoundary(twin) || !getMesh().toTriangle(twin).intersects(bbox)));
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
		P p1 = getMesh().getPoint(edge);
		P p2 = getMesh().getPoint(getMesh().getNext(edge));
		P p3 = getMesh().getPoint(getMesh().getNext(getMesh().getTwin(edge)));

		IPoint midPoint = getMesh().toLine(edge).midPoint();

		double len = Math.max(midPoint.distance(p1), midPoint.distance(p2));
		len = Math.max(len, midPoint.distance(p3));

		return distFunc.apply(midPoint) >= len;
	}

    private String curveToTikz() {
		return TexGraphGenerator.toTikz(getMesh(), sfc.asList().stream().map(node -> getMesh().getFace(node.getEdge())).collect(Collectors.toList()));
    }
}
