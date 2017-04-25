package org.vadere.util.geometry.mesh.triangulations;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.inter.IFace;
import org.vadere.util.geometry.mesh.inter.IHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.triangulation.IPointConstructor;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 */
public class UniformRefinementTriangulation<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> {
	private final Collection<VShape> boundary;
	private final VRectangle bbox;
	private final IEdgeLengthFunction lenFunc;
	private IncrementalTriangulation<P, E, F> triangulation;
	private Set<P> points;
	private IMesh<P, E, F> mesh;
	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulation.class);

	public UniformRefinementTriangulation(
			final IMesh<P, E, F> mesh,
			final double minX,
			final double minY,
			final double width,
			final double height,
			final Collection<VShape> boundary,
			final IEdgeLengthFunction lenFunc) {
		this.mesh = mesh;
		this.triangulation = new IncrementalTriangulation<>(mesh, minX, minY, width, height);
		this.boundary = boundary;
		this.lenFunc = lenFunc;
		this.bbox = new VRectangle(minX, minY, width, height);
		this.points = new HashSet<>();
	}

	public boolean isCompleted(final E edge) {
		F face = mesh.isBoundary(edge) ? mesh.getTwinFace(edge) : mesh.getFace(edge);
		VTriangle triangle = mesh.toTriangle(face);
		P end = mesh.getVertex(edge);
		P begin = mesh.getVertex(mesh.getPrev(edge));
		return !bbox.intersect(triangle) || boundary.stream().anyMatch(shape -> shape.contains(triangle.getBounds2D())) || begin.distance(end) <= lenFunc.apply(midPoint(edge));
	}

	public void compute() {
		logger.info("start computation");
		LinkedList<E> toRefineEdges = new LinkedList<>();
		List<E> edges = mesh.getEdges(triangulation.superTriangle);

		for(E edge : edges) {
			if(!isCompleted(edge) && !points.contains(mesh.getVertex(edge))) {
				toRefineEdges.add(edge);
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

		for(VShape shape : boundary) {
			// 1. find a triangle inside the boundary
			Optional<F> optFace = triangulation.locate(shape.getCentroid());

			if(optFace.isPresent()) {
				LinkedList<F> candidates = new LinkedList<>();
				candidates.add(optFace.get());

				// 2. as long as there is a face which has a vertex inside the shape remove it
				while (!candidates.isEmpty()) {
					F face = candidates.removeFirst();
					if(mesh.streamEdges(face).map(mesh::getVertex).anyMatch(shape::contains)) {
						mesh.streamFaces(face).forEach(f -> candidates.add(f));
						triangulation.removeFace(face, false);
					}
				}
			}
			else {
				logger.warn("no face found");
			}
		}
		logger.info("end computation");
	}

	public Set<VLine> getEdges() {
		return triangulation.getEdges();
	}

	public Collection<E> refine(final E edge) {
		VPoint midPoint = midPoint(edge);
		P p = mesh.createVertex(midPoint.getX(), midPoint.getY());

		if(points.contains(p)) {
			return Collections.emptyList();
		}
		else {
			points.add(p);
			mesh.insert(p);
			return triangulation.splitEdge(p, edge);
		}
	}

	private VPoint midPoint(final E edge) {
		VPoint p1 = new VPoint(mesh.getVertex(edge));
		VPoint p2 = new VPoint(mesh.getVertex(mesh.getPrev(edge)));
		return p2.add(p1).scalarMultiply(0.5);
	}

}
