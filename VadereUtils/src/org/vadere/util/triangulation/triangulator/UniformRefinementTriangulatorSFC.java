package org.vadere.util.triangulation.triangulator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.inter.*;
import org.vadere.util.geometry.shapes.*;
import org.vadere.util.tex.TexGraphGenerator;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Triangulation creator: This class is realises an algorithm which refine a given triangulation
 * (which might be empty), by recursively splitting existing triangles (starting with the super triangle if
 * the triangulation is empty) into parts. The class is a Functional.
 *
 * @author Benedikt Zoennchen
 *
 * @param <P>
 */
public class UniformRefinementTriangulatorSFC<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulator<P, V, E, F> {
	private final Collection<? extends VShape> boundary;
	private final VRectangle bbox;
	private final IEdgeLengthFunction lenFunc;
	private ITriangulation<P, V, E, F> triangulation;
	private final IMeshSupplier<P, V, E, F> meshSupplier;
	private Set<P> points;
	private ArrayList<SFCNode<P, V, E, F>> candidates;
	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulatorSFC.class);
	private final IDistanceFunction distFunc;
	private int counter = 0;
	private boolean finished;
	private final Collection<P> fixPoints;
	private final Random random = new Random();
	private final Map<E, SFCNode<P, V, E, F>> edgeToNode;
	private final SpaceFillingCurve<P, V, E, F> sfc;
	private final IMesh<P, V, E, F> mesh;

	/**
     * @param meshSupplier          a {@link IMeshSupplier}
     * @param bound                 the bounding box containing all boundaries and the topography with respect to the distance function distFunc
     * @param boundary              the boundaries e.g. obstacles
     * @param lenFunc               a edge length function
     * @param distFunc              a signed distance function
     */
	public UniformRefinementTriangulatorSFC(
			final IMeshSupplier<P, V, E, F> meshSupplier,
			final VRectangle bound,
			final Collection<? extends VShape> boundary,
			final IEdgeLengthFunction lenFunc,
			final IDistanceFunction distFunc,
			final Collection<P> fixPoints) {

		this.meshSupplier = meshSupplier;
	    this.distFunc = distFunc;
		this.boundary = boundary;
		this.lenFunc = lenFunc;
		this.bbox = bound;
		this.fixPoints = fixPoints;
		this.points = new HashSet<>();
		this.edgeToNode = new HashMap<>();
		this.candidates = new ArrayList<>();
		this.sfc = new SpaceFillingCurve<>();
		this.mesh = meshSupplier.get();
	}

    public ITriangulation<P, V, E, F> init() {
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
	    triangulation = mesh.toTriangulation(IPointLocator.Type.BASE);
	    edgeToNode.put(halfEdge, node1);
	    edgeToNode.put(getMesh().getTwin(halfEdge), node2);
	    return triangulation;
    }

    private IMesh<P, V, E, F> getMesh() {
    	return mesh;
    }

    private void nextSFCLevel(double ran) {
		//System.out.println(curveToTikz());
		ArrayList<SFCNode<P, V, E ,F>> newCandidates = new ArrayList<>(candidates.size() * 2);
	    Map<E, SFCNode<P, V, E, F>> newEdgeToNode = new HashMap<>();

	    ArrayList<E> toRefineEdges = new ArrayList<>();
		boolean tFinished = true;

		// 1. update CFS before refinement!
		for(SFCNode<P, V, E ,F> node : candidates) {
		    E edge = node.getEdge();
		    if((!isCompleted(edge) || random.nextDouble() < ran) && isLongestEdge(edge)) {
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

				newEdgeToNode.put(t1, element1);
				newEdgeToNode.put(t2, element2);
			}
			else {
				newEdgeToNode.put(edge, node);
			}
	    }

	    // 2. refine
	    for(E edge : toRefineEdges) {
			// to avoid 2x split
		    if(validEdge(edge)) {
				refine(edge);
		    }
	    }
		finished = tFinished;
	    candidates = newCandidates;
    }

    private void addToCurve(@NotNull final SFCNode<P, V, E, F> node, @NotNull final ArrayList<SFCNode<P, V, E, F>> sierpinskyCurve, final double ran) {
	    E edge = node.getEdge();

	    if(!isCompleted(edge) || random.nextDouble() < ran) {
			E twin = getMesh().getTwin(edge);
		    SFCDirection dir = node.getDirection();
		    E t1 = getMesh().getNext(edge);
		    E t2 = getMesh().getPrev(edge);

		    SFCNode<P, V, E ,F> element1 = new SFCNode<>(t1, dir.next());
		    SFCNode<P, V, E ,F> element2 = new SFCNode<>(t2, dir.next());

		    if(dir == SFCDirection.FORWARD) {
			    sierpinskyCurve.add(element2);
			    sierpinskyCurve.add(element1);
		    }
		    else {
			    sierpinskyCurve.add(element1);
			    sierpinskyCurve.add(element2);
		    }
	    }
    }

    public void step() {
		nextSFCLevel(0.0);
    }

	private boolean validEdge(@NotNull E edge) {
		if (getMesh().isAtBoundary(edge)) {
			return true;
		}
		P p1 = getMesh().getPoint(getMesh().getPrev(edge));
		P p2 = getMesh().getPoint(edge);
		return (p1.getX() > p2.getX() || (p1.getX() == p2.getX() && p1.getY() > p2.getY()));
	}


    public boolean isFinished() {
        return finished;
    }

	public ITriangulation<P, V, E, F> getTriangulation() {
		return triangulation;
	}

	public ITriangulation<P, V, E, F> generate() {
        logger.info("start triangulation generation");
        init();

		while (!isFinished()) {
			step();
		}

		nextSFCLevel(0.8);
        finish();
		logger.info("end triangulation generation");
		return triangulation;
	}

	public void finish() {
        synchronized (getMesh()) {
	        finished = true;
			List<F> sierpinksyFaceOrder = candidates.stream().map(node -> getMesh().getFace(node.getEdge())).collect(Collectors.toList());

			// insert special fix points
	        // TODO: adjust sierpinsky order, idea: construct a tree -> locate the face using the tree -> replace the face by the three new faces
			triangulation.insert(fixPoints);
			triangulation.finish();
			removeTrianglesOutsideBBox();
			removeTrianglesInsideObstacles();

	        sierpinksyFaceOrder.removeIf(face -> getMesh().isDestroyed(face) || getMesh().isHole(face));
	        List<F> holes = getMesh().streamHoles().collect(Collectors.toList());
	        logger.info("#holes:" + holes.size());
	        sierpinksyFaceOrder.addAll(holes);
	        logger.info(sierpinksyFaceOrder.size() + ", " + getMesh().getNumberOfFaces());

		    getMesh().arrangeMemory(sierpinksyFaceOrder);
        }
    }

    private E getLongestEdge(F face) {
	    return getMesh().streamEdges(face).reduce((e1, e2) -> getMesh().toLine(e1).length() > getMesh().toLine(e2).length() ? e1 : e2).get();
    }

    private boolean isLongestEdge(E edge) {
		if(!getMesh().isAtBorder(edge)) {
			E longestEdge1 = getMesh().streamEdges(getMesh().getFace(edge)).reduce((e1, e2) -> getMesh().toLine(e1).length() > getMesh().toLine(e2).length() ? e1 : e2).get();
			E longestEdge2 = getMesh().streamEdges(getMesh().getTwinFace(edge)).reduce((e1, e2) -> getMesh().toLine(e1).length() > getMesh().toLine(e2).length() ? e1 : e2).get();
			return getMesh().isSame(longestEdge1, edge) && getMesh().isSame(longestEdge2, edge);
		}
	    else {
			if(getMesh().isBoundary(edge)) {
				edge = getMesh().getTwin(edge);
			}

			E longestEdge = getMesh().streamEdges(getMesh().getFace(edge)).reduce((e1, e2) -> getMesh().toLine(e1).length() > getMesh().toLine(e2).length() ? e1 : e2).get();
			return getMesh().isSame(longestEdge, edge);
		}
    }

	public void removeTrianglesOutsideBBox() {
		triangulation.shrinkBorder(f -> distFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
	}

	public void removeTrianglesInsideObstacles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> distFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0.01, true);
			}
		}
	}

	/**
	 * Tests if a specific edge is complete i.e. it should not be split into two edges.
	 *
	 * @param edge the half-edge representing the edge of the mesh.
	 * @return true, if the edge should not be split, false otherwise
	 */
	private boolean isCompleted(E edge) {
		if(getMesh().isBoundary(edge)){
			edge = getMesh().getTwin(edge);
		}

		return isSmallEnough(edge) /*|| isEdgeOutsideBBox(edge) || isEdgeInsideHole(edge);*/;
	}

	private boolean isSmallEnough(@NotNull final E edge) {
		VLine line = getMesh().toLine(edge);
		return (line.length() <= lenFunc.apply(line.midPoint()));
	}

	private boolean isEdgeOutsideBBox(@NotNull final E edge) {
		F face = getMesh().getFace(edge);
		F twin = getMesh().getTwinFace(edge);

		VTriangle triangle = getMesh().toTriangle(face);
		return (!triangle.intersect(bbox) && (getMesh().isBoundary(twin) || !getMesh().toTriangle(twin).intersect(bbox)));
	}

	private boolean isEdgeInsideHole(@NotNull final E edge) {
		F face = getMesh().getFace(edge);
		F twin = getMesh().getTwinFace(edge);

		VTriangle triangle = getMesh().toTriangle(face);
		return boundary.stream().anyMatch(shape -> shape.contains(triangle.getBounds2D()) || (!getMesh().isBoundary(twin) && shape.contains(getMesh().toTriangle(twin).getBounds2D())));
	}

    private void refine(final E edge) {
        IPoint midPoint = getMesh().toLine(edge).midPoint();
        P p = getMesh().createPoint(midPoint.getX(), midPoint.getY());

        if(!points.contains(p)) {
            points.add(p);
            E newEdge = triangulation.getAnyEdge( triangulation.splitEdge(p, edge, true));
	        triangulation.insertEvent(newEdge);
        }
        else {
            throw new IllegalStateException(p + " point already exist.");
        }
    }

    private String curveToTikz() {
		return TexGraphGenerator.toTikz(getMesh(), sfc.asList().stream().map(node -> getMesh().getFace(node.getEdge())).collect(Collectors.toList()));
    }

	/*private void removeTrianglesInsideObstacles() {
		for(VShape shape : boundary) {

			// 1. find a triangle inside the boundary
			VPoint centroid = shape.getCentroid();

			Optional<F> optFace = triangulation.locateFace(centroid.getX(), centroid.getY());

			if(optFace.isPresent()) {
				LinkedList<F> candidates = new LinkedList<>();
				candidates.add(optFace.get());

				// 2. as long as there is a face which has a vertex inside the shape remove it
				while (!candidates.isEmpty()) {
					F face = candidates.removeFirst();

					if(!mesh.isDestroyed(face) && mesh.streamEdges(face).map(mesh::toLine).anyMatch(line -> intersectShape(line, shape))) {
						mesh.streamFaces(face)
								//.filter(f -> !face.equals(f)).distinct()
								.forEach(candidate -> candidates.addFirst(candidate));
						triangulation.removeFaceAtBorder(face, true);
					}
				}
			}
			else {
				logger.warn("no face found");
			}
		}
	}

	private boolean intersectShape(final VLine line, final VShape shape) {
		return shape.intersects(line) || shape.contains(line.getP1()) || shape.contains(line.getP2());
	}*/
}
