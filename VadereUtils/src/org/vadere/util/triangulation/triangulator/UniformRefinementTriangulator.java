package org.vadere.util.triangulation.triangulator;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
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
public class UniformRefinementTriangulator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulator<P, V, E, F> {
	private final Collection<? extends VShape> boundary;
	private final VRectangle bbox;
	private final IEdgeLengthFunction lenFunc;
	private ITriangulation<P, V, E, F>  triangulation;
	private Set<P> points;
	private IMesh<P, V, E, F> mesh;
	private  LinkedList<F> toRefineEdges;
	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulator.class);
	private final IDistanceFunction distFunc;
	private final Map<P,Integer> creationOrder;

    /**
     * @param supplier
     * @param bound         the bounding box containing all boundaries and the topography with respect to the distance function distFunc
     * @param boundary      the boundaries e.g. obstacles
     * @param lenFunc       a edge length function
     * @param distFunc      a signed distance function
     */
	public UniformRefinementTriangulator(
			final ITriangulationSupplier<P, V, E, F> supplier,
			final VRectangle bound,
			final Collection<? extends VShape> boundary,
			final IEdgeLengthFunction lenFunc,
			final IDistanceFunction distFunc) {

	    this.distFunc = distFunc;
		this.triangulation = supplier.get();
		this.mesh = triangulation.getMesh();
		this.boundary = boundary;
		this.lenFunc = lenFunc;
		this.bbox = bound;
		this.points = new HashSet<>();
        this.toRefineEdges = new LinkedList<>();
        this.creationOrder = new HashMap<>();
        toRefineEdges.addAll(mesh.getFaces());
	}

    public void init() {
        triangulation.init();
    }

    public void step() {
	    synchronized (mesh) {
            if (!toRefineEdges.isEmpty()) {
                F face = toRefineEdges.removeLast();
                E edge = getLongestEdge(face);

                if(!isCompleted(edge)) {
                    refine(edge);
                }
            }
        }
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

    public void finish() {
        synchronized (mesh) {
            removeTrianglesOutsideBBox();
            removeTrianglesInsideObstacles();
            triangulation.finish();
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
            creationOrder.put(p, points.size()-1);
            Pair<E, E> createdEdges = triangulation.splitEdge(p, edge, true);
            List<F> createdFaces = new ArrayList<>(4);


            if(createdEdges.getRight() != null) {
                E e1 = createdEdges.getRight();
                E e2 = mesh.getTwin(createdEdges.getRight());

                F f1 = mesh.getFace(e1);
                F f2 = mesh.getFace(e2);

                toRefineEdges.addFirst(f1);
                toRefineEdges.addFirst(f2);

            }


            if(createdEdges.getLeft() != null) {
                E e1 = createdEdges.getLeft();
                E e2 = mesh.getTwin(createdEdges.getLeft());

                Integer index1 = creationOrder.get(mesh.getPoint(mesh.getNext(e1)));
                Integer index2 = creationOrder.get(mesh.getPoint(mesh.getNext(e2)));


                toRefineEdges.addFirst(mesh.getFace(e2));
                toRefineEdges.addFirst(mesh.getFace(e1));

            }
        }
    }

	/*private IPoint midPoint(final E edge) {
		P p1 = mesh.getPoint(edge);
		P p2 = mesh.getPoint(mesh.getPrev(edge));
		return p2.add(p1).scalarMultiply(0.5);
	}*/

}
