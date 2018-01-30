package org.vadere.util.triangulation.triangulator;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.mesh.inter.*;
import org.vadere.util.geometry.shapes.*;
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
public class AUniformRefinementTriangulator<P extends IPoint> implements ITriangulator {
	private final Collection<VShape> boundary;
	private final VRectangle bbox;
	private final IEdgeLengthFunction lenFunc;
	private ITriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>> triangulation;
	private Set<P> points;
	private IMesh<P, AVertex<P>, AHalfEdge<P>, AFace<P>> mesh;
	private LinkedList<AFace<P>> toRefineEdges;
	private static final Logger logger = LogManager.getLogger(AUniformRefinementTriangulator.class);
	private final IDistanceFunction distFunc;

    /**
     * @param triangulation an empty triangulation to fill
     * @param bound         the bounding box containing all boundaries and the topography with respect to the distance function distFunc
     * @param boundary      the boundaries e.g. obstacles
     * @param lenFunc       a edge length function
     * @param distFunc      a signed distance function
     */
	public AUniformRefinementTriangulator(
			final ITriangulation<P, AVertex<P>, AHalfEdge<P>, AFace<P>> triangulation,
			final VRectangle bound,
			final Collection<VShape> boundary,
			final IEdgeLengthFunction lenFunc,
			final IDistanceFunction distFunc) {

	    this.distFunc = distFunc;
		this.triangulation = triangulation;
		this.mesh = triangulation.getMesh();
		this.boundary = boundary;
		this.lenFunc = lenFunc;
		this.bbox = bound;
		this.points = new HashSet<>();
        this.toRefineEdges = new LinkedList<>();
        toRefineEdges.addAll(mesh.getFaces());
	}

	private class FaceComparator implements Comparator<AFace<P>> {

        @Override
        public int compare(AFace<P> f1, AFace<P> f2) {

            int s1 = mesh.streamVertices(f1).mapToInt(v -> getIndex(v)).sum();
            int s2 = mesh.streamVertices(f2).mapToInt(v -> getIndex(v)).sum();

            if(s1 < s2) {
                return 1;
            }
            else if(s1 > s2) {
                return -1;
            }
            else {
                int min1 = mesh.streamVertices(f1).mapToInt(v -> getIndex(v)).min().getAsInt();
                int min2 = mesh.streamVertices(f2).mapToInt(v -> getIndex(v)).min().getAsInt();

                if(min2 < min1) {
                    return 1;
                }
                else if(min2 > min1) {
                    return -1;
                }
                else {
                    return 0;
                }
            }
        }

        private int getIndex(final AVertex<P> vertex) {
            return vertex.getId();
        }

    }

    public void init() {
        synchronized (mesh) {
            triangulation.init();
        }
    }

    public void finalize() {
        synchronized (mesh) {
            triangulation.finalize();
        }
    }

    public boolean isFinished() {
	    return toRefineEdges.isEmpty();
    }

    public void step() {
	    synchronized (mesh) {
            if (!toRefineEdges.isEmpty()) {
                AFace<P> face = toRefineEdges.removeFirst();
                AHalfEdge<P> edge = getLongestEdge(face);

                if(!isCompleted(edge)) {
                    refine(edge);
                }
            }
        }
    }

	public void generate() {
        triangulation.init();

		logger.info("start triangulation generation");

		while (!toRefineEdges.isEmpty()) {
            AFace<P> face = toRefineEdges.removeFirst();
			AHalfEdge<P> edge = getLongestEdge(face);

			if(!isCompleted(edge)) {
			    // refine adds elements to toRefinedEdges.
			    refine(edge);
            }
		}

		//removeTrianglesOutsideBBox();
		//removeTrianglesInsideObstacles();
		//triangulation.finalize();
		logger.info("end triangulation generation");
	}

	private AHalfEdge<P> getLongestEdge(AFace<P> face) {
	    return mesh.streamEdges(face).reduce((e1, e2) -> mesh.toLine(e1).length() > mesh.toLine(e2).length() ? e1 : e2).get();
    }

    private AHalfEdge<P> getLatestEdge(AFace<P> face) {
        return mesh.streamEdges(face).reduce((e1, e2) -> mesh.getVertex(e1).getId() + mesh.getVertex(mesh.getPrev(e1)).getId() > mesh.getVertex(e2).getId() + mesh.getVertex(mesh.getPrev(e2)).getId() ? e1 : e2).get();
    }

	private void removeTrianglesOutsideBBox() {
		boolean removedSome = true;

		while (removedSome) {
			removedSome = false;

			List<AFace<P>> candidates = mesh.getFaces(mesh.getBoundary());
			for(AFace<P> face : candidates) {
				if(!mesh.isDestroyed(face) && mesh.streamVertices(face).anyMatch(v -> !bbox.contains(v))) {
					triangulation.removeFace(face, true);
					removedSome = true;
				}
			}
		}
	}

	private void removeTrianglesInsideObstacles() {
		List<AFace<P>> faces = triangulation.getMesh().getFaces();
		for(AFace<P> face : faces) {
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

	private boolean isCompleted(AHalfEdge<P> edge) {
		if(mesh.isBoundary(edge)){
			edge = mesh.getTwin(edge);
		}

        AFace<P> face = mesh.getFace(edge);
        AFace<P> twin = mesh.getTwinFace(edge);

		VTriangle triangle = mesh.toTriangle(face);
		VLine line = mesh.toLine(edge);

		return (line.length() <= lenFunc.apply(line.midPoint())); /*
				|| (!triangle.intersect(bbox) && (mesh.isBoundary(twin) || !mesh.toTriangle(twin).intersect(bbox)))
				|| boundary.stream().anyMatch(shape -> shape.contains(triangle.getBounds2D()) || (!mesh.isBoundary(twin) && shape.contains(mesh.toTriangle(twin).getBounds2D())));
	*/
	}

	private void refine(final AHalfEdge<P> edge) {
		IPoint midPoint = mesh.toLine(edge).midPoint();
		P p = mesh.createPoint(midPoint.getX(), midPoint.getY());

		if(!points.contains(p)) {
            points.add(p);
            Pair<AHalfEdge<P>, AHalfEdge<P>> createdEdges = triangulation.splitEdge(p, edge, true);
            List<AFace<P>> createdFaces = new ArrayList<>(4);


            if(createdEdges.getLeft() != null) {
                AHalfEdge<P> e1 = createdEdges.getLeft();
                AHalfEdge<P> e2 = mesh.getTwin(createdEdges.getLeft());

                if(mesh.getVertex(mesh.getNext(e1)).getId() > mesh.getVertex(mesh.getNext(e2)).getId()) {
                    toRefineEdges.addFirst(mesh.getFace(e2));
                    toRefineEdges.addFirst(mesh.getFace(e1));
                }
                else {
                    toRefineEdges.addFirst(mesh.getFace(e1));
                    toRefineEdges.addFirst(mesh.getFace(e2));
                }

            }

            /*if(createdEdges.getRight() != null) {
                AHalfEdge<P> e1 = createdEdges.getRight();
                AHalfEdge<P> e2 = mesh.getTwin(createdEdges.getRight());

                AFace<P> f1 = mesh.getFace(e1);
                AFace<P> f2 = mesh.getFace(e2);

                toRefineEdges.addFirst(f1);
                toRefineEdges.addFirst(f2);
            }*/

            if(createdEdges.getRight() != null) {
                AHalfEdge<P> e1 = createdEdges.getRight();
                AHalfEdge<P> e2 = mesh.getTwin(createdEdges.getRight());

                AFace<P> f1 = mesh.getFace(e1);
                AFace<P> f2 = mesh.getFace(e2);

                int index1 = toRefineEdges.indexOf(f1);
                int index2 = toRefineEdges.indexOf(f2);

                assert (index1 != -1 || index2 != -1) && (index1 == -1 || index2 == -1);

                if(index1 != -1) {
                    toRefineEdges.add(index1+1, f2);
                }
                else {
                    toRefineEdges.add(index2+1, f1);
                }
            }
		}
	}

	/*private IPoint midPoint(final E edge) {
		P p1 = mesh.getPoint(edge);
		P p2 = mesh.getPoint(mesh.getPrev(edge));
		return p2.add(p1).scalarMultiply(0.5);
	}*/

}
