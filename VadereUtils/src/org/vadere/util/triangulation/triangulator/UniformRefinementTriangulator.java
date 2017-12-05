package org.vadere.util.triangulation.triangulator;

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
import org.vadere.util.triangulation.adaptive.IDistanceFunction;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Triangulation creator: This class is realises an algorithm which refine a given triangulation
 * (which might be empty), by recursively splitting existing triangles (starting with the super triangle if
 * the triangulation is empty) into parts. The class is a Functional.
 *
 * @author Benedikt Zoennchen
 *
 * @param <P>
 */
public class UniformRefinementTriangulator<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> implements ITriangulator {
	private final Collection<VShape> boundary;
	private final VRectangle bbox;
	private final IEdgeLengthFunction lenFunc;
	private ITriangulation<P, V, E, F>  triangulation;
	private Set<P> points;
	private IMesh<P, V, E, F> mesh;
	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulator.class);
	private final IDistanceFunction distFunc;
	private final static Random random = new Random();

    /**
     * @param triangulation an empty triangulation to fill
     * @param bound         the bounding box containing all boundaries and the topography with respect to the distance function distFunc
     * @param boundary      the boundaries e.g. obstacles
     * @param lenFunc       a edge length function
     * @param distFunc      a signed distance function
     */
	public UniformRefinementTriangulator(
			final ITriangulation<P, V, E, F> triangulation,
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
	}

	public void generate() {
		triangulation.init();

		logger.info("start triangulation generation");
		LinkedList<E> toRefineEdges = new LinkedList<>();

		for(E edge : mesh.getEdgeIt(mesh.getBoundary())) {
			if(!isCompleted(edge) && !points.contains(mesh.getVertex(edge))) {
				toRefineEdges.add(mesh.getTwin(edge));
			}
		}

		while (!toRefineEdges.isEmpty()) {
			E edge = toRefineEdges.removeFirst();
			for(E refinedHalfEdges : refine(edge)) {
				if(!isCompleted(refinedHalfEdges)) {
					toRefineEdges.addLast(refinedHalfEdges);
				}
			}
		}

		removeTrianglesOutsideBBox();
		removeTrianglesInsideObstacles();
		triangulation.finalize();
		logger.info("end triangulation generation");
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

		return (line.length() <= lenFunc.apply(line.midPoint()) && random.nextDouble() < 0.96)
				|| (!triangle.intersect(bbox) && (mesh.isBoundary(twin) || !mesh.toTriangle(twin).intersect(bbox)))
				|| boundary.stream().anyMatch(shape -> shape.contains(triangle.getBounds2D()) || (!mesh.isBoundary(twin) && shape.contains(mesh.toTriangle(twin).getBounds2D())));
	}

	private Collection<E> refine(final E edge) {
		IPoint midPoint = mesh.toLine(edge).midPoint();
		P p = mesh.createPoint(midPoint.getX(), midPoint.getY());

		if(points.contains(p)) {
			return Collections.emptyList();
		}
		else {
			points.add(p);
			E createdEdge = triangulation.splitEdge(p, edge, false);
			return mesh.getIncidentEdges(createdEdge);
		}
	}

	/*private IPoint midPoint(final E edge) {
		P p1 = mesh.getPoint(edge);
		P p2 = mesh.getPoint(mesh.getPrev(edge));
		return p2.add(p1).scalarMultiply(0.5);
	}*/

}
