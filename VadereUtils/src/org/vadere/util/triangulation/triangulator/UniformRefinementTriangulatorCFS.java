package org.vadere.util.triangulation.triangulator;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.data.Node;
import org.vadere.util.data.NodeLinkedList;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.mesh.gen.AFace;
import org.vadere.util.geometry.mesh.gen.AHalfEdge;
import org.vadere.util.geometry.mesh.gen.AMesh;
import org.vadere.util.geometry.mesh.inter.*;
import org.vadere.util.geometry.shapes.*;
import org.vadere.util.triangulation.ITriangulationSupplier;
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;

import java.util.*;

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
	private LinkedList<Node<Pair<Direction, E>>> toRefineEdges;
    private NodeLinkedList<Pair<Direction, E>> orderedFaces;
	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulatorCFS.class);
	private final IDistanceFunction distFunc;

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

        E halfEdge = getLongestEdge(mesh.getFace());
        orderedFaces.add(Pair.of(Direction.FORWARD, halfEdge));
        toRefineEdges.add(orderedFaces.getHead());
	}

    public ITriangulation<P, V, E, F> init() {
        triangulation.init();
        return triangulation;
    }

    public void step() {
	    synchronized (mesh) {
            if (!toRefineEdges.isEmpty()) {
                Node<Pair<Direction, E>> node = toRefineEdges.removeFirst();
                Pair<Direction, E> splitCandidate = node.getElement();

                Direction dir = splitCandidate.getLeft();
                E halfEdge = splitCandidate.getRight();

                if(!isCompleted(halfEdge)) {
                    addEdge(dir, halfEdge, node);

                    if(!mesh.isBoundary(mesh.getTwin(halfEdge))) {
                        addEdge(dir, mesh.getTwin(halfEdge), node);
                    }

                    node.remove();
                    refine(halfEdge);
                }
            }
        }
    }

    private void addEdge(Direction dir, E edge, Node<Pair<Direction, E>> node) {
        E t1 = mesh.getNext(edge);
        E t2 = mesh.getPrev(edge);

        Pair<Direction, E> element3 = Pair.of(dir.next(), t1);
        Pair<Direction, E> element4 = Pair.of(dir.next(), t2);

        if(dir == Direction.BACKWARD) {
            Pair<Direction, E> tmp = element3;
            element3 = element4;
            element4 = tmp;
        }

        // avoid duplicates due to twins
        if(validEdge(element3.getRight())) {
            Node<Pair<Direction, E>> node3 = orderedFaces.insertNext(element3, node);
            toRefineEdges.addLast(node3);
        }

        if(validEdge(element4.getRight())){
            Node<Pair<Direction, E>> node4 = orderedFaces.insertNext(element4, node);
            toRefineEdges.addLast(node4);
        }
    }

    private boolean validEdge(@NotNull E edge) {
	    return mesh.isAtBoundary(edge) || mesh.getPoint(edge).distanceToOrigin() > mesh.getPoint(mesh.getPrev(edge)).distanceToOrigin();
    }

    public boolean isFinished() {
        return toRefineEdges.isEmpty();
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
            //triangulation.finish();
            removeTrianglesOutsideBBox();
            removeTrianglesInsideObstacles();
            triangulation.finish();

            // TODO: dirty type casting?
            if(mesh instanceof AMesh) {
                List<AFace<P>> faceOrder = new ArrayList<>(mesh.getNumberOfFaces());
                AMesh<P> aMesh = ((AMesh<P>)mesh);

                for(Pair<Direction, E> element : orderedFaces.elementIterable()) {
                    AHalfEdge<P> halfEdge = (AHalfEdge<P>) element.getRight();

                    // due to triangulation.finish();
                    if(!aMesh.isDestroyed(halfEdge)) {
                        if(!aMesh.isBoundary(halfEdge)) {
                            faceOrder.add(aMesh.getFace((halfEdge)));
                        }

                        AHalfEdge<P> twin = aMesh.getTwin(halfEdge);
                        if(!aMesh.isBoundary(twin)) {
                            faceOrder.add(aMesh.getFace(twin));
                        }
                    }
                }

                logger.info(faceOrder.size() + ", " + mesh.getNumberOfFaces());

                aMesh.rearrange(() -> faceOrder.iterator());
            }
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
				    triangulation.removeFace(face, true);
					removedSome = true;
				}

			}
		}
	}

	private void removeTrianglesInsideObstacles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && distFunc.apply(triangulation.getMesh().toTriangle(face).midPoint()) > 0) {
				triangulation.removeFace(face, true);
			}
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
						triangulation.removeFace(face, true);
					}
				}
			}
			else {
				logger.warn("no face found");
			}
		}
	}*/

	private boolean intersectShape(final VLine line, final VShape shape) {
		return shape.intersects(line) || shape.contains(line.getP1()) || shape.contains(line.getP2());
	}

	private boolean isCompleted(E edge) {
		if(mesh.isBoundary(edge)){
			edge = mesh.getTwin(edge);
		}

		F face = mesh.getFace(edge);
		F twin = mesh.getTwinFace(edge);

		VTriangle triangle = mesh.toTriangle(face);
		VLine line = mesh.toLine(edge);

		return (line.length() <= lenFunc.apply(line.midPoint()));/* && random.nextDouble() < 0.96)
				|| (!triangle.intersect(bbox) && (mesh.isBoundary(twin) || !mesh.toTriangle(twin).intersect(bbox)))
				|| boundary.stream().anyMatch(shape -> shape.contains(triangle.getBounds2D()) || (!mesh.isBoundary(twin) && shape.contains(mesh.toTriangle(twin).getBounds2D())));
	*/
	}

    private void refine(final E edge) {
        IPoint midPoint = mesh.toLine(edge).midPoint();
        P p = mesh.createPoint(midPoint.getX(), midPoint.getY());

        if(!points.contains(p)) {
            points.add(p);
            P splitPoint = mesh.getPoint(mesh.getNext(edge));
            triangulation.splitEdge(p, edge, false);
        }
        else {
            throw new IllegalStateException("wtf");
        }
    }
}
