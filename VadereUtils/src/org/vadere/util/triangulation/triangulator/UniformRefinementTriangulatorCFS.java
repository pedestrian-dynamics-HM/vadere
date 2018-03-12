package org.vadere.util.triangulation.triangulator;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.inter.*;
import org.vadere.util.geometry.shapes.*;
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
public class UniformRefinementTriangulatorCFS<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulator<P, V, E, F> {
	private final Collection<? extends VShape> boundary;
	private final VRectangle bbox;
	private final IEdgeLengthFunction lenFunc;
	private ITriangulation<P, V, E, F> triangulation;
	private final IMeshSupplier<P, V, E, F> meshSupplier;
	private Set<P> points;
	private ArrayList<Pair<Direction, E>> sierpinskyCurve;
	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulatorCFS.class);
	private final IDistanceFunction distFunc;
	private int counter = 0;
	private boolean finished;
	private final Collection<P> fixPoints;

	enum Direction {
	    FORWARD,
        BACKWARD;
        public Direction next() {
	        return this == FORWARD ? BACKWARD : FORWARD;
        }
    }

    /**
     * @param meshSupplier          a {@link IMeshSupplier}
     * @param bound                 the bounding box containing all boundaries and the topography with respect to the distance function distFunc
     * @param boundary              the boundaries e.g. obstacles
     * @param lenFunc               a edge length function
     * @param distFunc              a signed distance function
     */
	public UniformRefinementTriangulatorCFS(
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
	}

    public ITriangulation<P, V, E, F> init() {

    	IMesh<P, V, E, F> mesh = meshSupplier.get();
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

	    this.triangulation = mesh.toTriangulation(IPointLocator.Type.BASE);
	    E halfEdge = getLongestEdge(mesh.getFace());
	    this.sierpinskyCurve = new ArrayList<>();
	    this.sierpinskyCurve.add(Pair.of(Direction.FORWARD, halfEdge));
	    this.sierpinskyCurve.add(Pair.of(Direction.FORWARD, getMesh().getTwin(halfEdge)));
	    return triangulation;
    }

    private IMesh<P, V, E, F> getMesh() {
    	return triangulation.getMesh();
    }

    private void nextSFCLevel() {
		ArrayList<Pair<Direction, E>> newSierpinskiCurve = new ArrayList<>(sierpinskyCurve.size() * 2);

		boolean tFinished = true;
	    for(Pair<Direction, E> pair : sierpinskyCurve) {
		    E edge = pair.getRight();
		    if(!isCompleted(edge)) {
		    	tFinished = false;
				Direction dir = pair.getLeft();
				E t1 = getMesh().getNext(edge);
				E t2 = getMesh().getPrev(edge);

				Pair<Direction, E> element1 = Pair.of(dir.next(), t1);
				Pair<Direction, E> element2 = Pair.of(dir.next(), t2);

				if(dir == Direction.FORWARD) {
					newSierpinskiCurve.add(element2);
					newSierpinskiCurve.add(element1);
				}
				else {
					newSierpinskiCurve.add(element1);
					newSierpinskiCurve.add(element2);
				}
			}
			else {
				newSierpinskiCurve.add(pair);
			}
	    }

	    for(Pair<Direction, E> pair : sierpinskyCurve) {
		    E edge = pair.getRight();
		    if(!isCompleted(edge) && validEdge(edge)) {
				refine(edge);
		    }
	    }
		finished = tFinished;
	    sierpinskyCurve = newSierpinskiCurve;
    }

    public void step() {
		nextSFCLevel();
    }

	private boolean validEdge(@NotNull E edge) {
		P p1 = getMesh().getPoint(getMesh().getPrev(edge));
		P p2 = getMesh().getPoint(edge);
		return getMesh().isAtBoundary(edge) || (p1.getX() > p2.getX() || (p1.getX() == p2.getX() && p1.getY() > p2.getY()));
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

        finish();
		logger.info("end triangulation generation");
		return triangulation;
	}

	public void finish() {
        synchronized (getMesh()) {
			List<F> sierpinksyFaceOrder = sierpinskyCurve.stream().map(e -> getMesh().getFace(e.getRight())).collect(Collectors.toList());

			// insert special fix points
	        // TODO: adjust sierpinsky order, idea: construct a tree -> locate the face using the tree -> replace the face by the three new faces
			triangulation.insert(fixPoints);
			triangulation.finish();
			removeTrianglesOutsideBBox();
			removeTrianglesInsideObstacles();

	        sierpinksyFaceOrder.removeIf(face -> getMesh().isDestroyed(face));
	        List<F> holes = getMesh().streamHoles().collect(Collectors.toList());
	        logger.info("#holes:" + holes.size());
	        sierpinksyFaceOrder.addAll(holes);
	        logger.info(sierpinksyFaceOrder.size() + ", " + getMesh().getNumberOfFaces());

		     getMesh().arrangeMemory(() -> (Iterator<AFace<P>>) sierpinksyFaceOrder.iterator());
        }
    }

    private E getLongestEdge(F face) {
	    return getMesh().streamEdges(face).reduce((e1, e2) -> getMesh().toLine(e1).length() > getMesh().toLine(e2).length() ? e1 : e2).get();
    }

	public void removeTrianglesOutsideBBox() {
		triangulation.shrinkBorder(f -> distFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
	}

	public void removeTrianglesInsideObstacles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> distFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
			}
		}
	}

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
