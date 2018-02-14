package org.vadere.util.triangulation.triangulator;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.data.Node;
import org.vadere.util.data.NodeLinkedList;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.inter.*;
import org.vadere.util.geometry.shapes.*;
import org.vadere.util.triangulation.ITriangulationSupplier;
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
	private ITriangulation<P, V, E, F>  triangulation;
	private Set<P> points;
	private IMesh<P, V, E, F> mesh;
	private ArrayList<Pair<Direction, E>> sierpinskyCurve;
	private LinkedList<Node<Pair<Direction, E>>> toRefineEdges;
    private NodeLinkedList<Pair<Direction, E>> orderedFaces;
	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulatorCFS.class);
	private final IDistanceFunction distFunc;
	private int counter = 0;
	private boolean finished;

	enum Direction {
	    FORWARD,
        BACKWARD;

        public Direction next() {
	        return this == FORWARD ? BACKWARD : FORWARD;
        }
    }

    /**
     * @param triangulationSupplier a {@link ITriangulationSupplier}
     * @param bound                 the bounding box containing all boundaries and the topography with respect to the distance function distFunc
     * @param boundary              the boundaries e.g. obstacles
     * @param lenFunc               a edge length function
     * @param distFunc              a signed distance function
     */
	public UniformRefinementTriangulatorCFS(
			final ITriangulationSupplier<P, V, E, F> triangulationSupplier,
			final VRectangle bound,
			final Collection<? extends VShape> boundary,
			final IEdgeLengthFunction lenFunc,
			final IDistanceFunction distFunc) {

	    this.distFunc = distFunc;
		this.triangulation = triangulationSupplier.get();
		this.mesh = triangulation.getMesh();
		this.boundary = boundary;
		this.lenFunc = lenFunc;
		this.bbox = bound;
		this.points = new HashSet<>();
        this.toRefineEdges = new LinkedList<>();
        this.orderedFaces = new NodeLinkedList<>();
        this.sierpinskyCurve = new ArrayList<>(2);


        E halfEdge = getLongestEdge(mesh.getFace());
        sierpinskyCurve.add(Pair.of(Direction.FORWARD, halfEdge));
        sierpinskyCurve.add(Pair.of(Direction.FORWARD, mesh.getTwin(halfEdge)));
	}

    public ITriangulation<P, V, E, F> init() {
        triangulation.init();
        return triangulation;
    }

    private void nextSFCLevel() {
		ArrayList<Pair<Direction, E>> newSierpinskiCurve = new ArrayList<>(sierpinskyCurve.size() * 2);

		boolean tFinished = true;
	    for(Pair<Direction, E> pair : sierpinskyCurve) {
		    E edge = pair.getRight();
		    if(!isCompleted(edge)) {
		    	tFinished = false;
				Direction dir = pair.getLeft();
				E t1 = mesh.getNext(edge);
				E t2 = mesh.getPrev(edge);

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
		P p1 = mesh.getPoint(mesh.getPrev(edge));
		P p2 = mesh.getPoint(edge);
		return mesh.isAtBoundary(edge) || (p1.getX() > p2.getX() || (p1.getX() == p2.getX() && p1.getY() > p2.getY()));
	}


    public boolean isFinished() {
        return finished;
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

	public ITriangulation<P, V, E, F> getTri(){
	    return triangulation;
    }

	public void finish() {
        synchronized (mesh) {
			List<F> sierpinksyFaceOrder = sierpinskyCurve.stream().map(e -> mesh.getFace(e.getRight())).collect(Collectors.toList());

			triangulation.finish();
			removeTrianglesOutsideBBox();
			removeTrianglesInsideObstacles();

			sierpinksyFaceOrder.removeIf(face -> mesh.isDestroyed(face));
	        List<F> holes = mesh.streamHoles().collect(Collectors.toList());
			//sierpinksyFaceOrder.addAll(holes);
			logger.info(sierpinksyFaceOrder.size() + ", " + mesh.getNumberOfFaces());

			// TODO: dirty type casting?
			((AMesh<P>)mesh).rearrange(() -> (Iterator<AFace<P>>) sierpinksyFaceOrder.iterator());
        }
    }


    private E getLongestEdge(F face) {
	    return mesh.streamEdges(face).reduce((e1, e2) -> mesh.toLine(e1).length() > mesh.toLine(e2).length() ? e1 : e2).get();
    }

	private void removeTrianglesOutsideBBox() {
		boolean removedSome = true;
		while (removedSome) {
			removedSome = false;
            List<F> candidates = mesh.getFaces(mesh.getBoundary());
			for(F face : candidates) {

				if(!mesh.isDestroyed(face) && mesh.streamVertices(face).anyMatch(v -> !bbox.contains(v))) {
				    triangulation.removeBorderFace(face, true);
					removedSome = true;
				}

			}
		}
	}

	private void removeTrianglesInsideObstacles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && distFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
				triangulation.removeBorderFace(face, true);
			}
		}
	}

	private boolean isCompleted(E edge) {
		if(mesh.isBoundary(edge)){
			edge = mesh.getTwin(edge);
		}

		F face = mesh.getFace(edge);
		F twin = mesh.getTwinFace(edge);

		VTriangle triangle = mesh.toTriangle(face);
		VLine line = mesh.toLine(edge);

		return (line.length() <= lenFunc.apply(line.midPoint()))
				|| (!triangle.intersect(bbox) && (mesh.isBoundary(twin) || !mesh.toTriangle(twin).intersect(bbox)))
				|| boundary.stream().anyMatch(shape -> shape.contains(triangle.getBounds2D()) || (!mesh.isBoundary(twin) && shape.contains(mesh.toTriangle(twin).getBounds2D())));
	}

    private void refine(final E edge) {
        IPoint midPoint = mesh.toLine(edge).midPoint();
        P p = mesh.createPoint(midPoint.getX(), midPoint.getY());

        if(!points.contains(p)) {
            points.add(p);
            triangulation.splitEdge(p, edge, false);
        }
        else {
            throw new IllegalStateException("point already exist.");
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
						triangulation.removeBorderFace(face, true);
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
