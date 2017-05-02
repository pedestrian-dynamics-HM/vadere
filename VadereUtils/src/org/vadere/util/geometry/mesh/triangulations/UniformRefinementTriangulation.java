package org.vadere.util.geometry.mesh.triangulations;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.impl.PFace;
import org.vadere.util.geometry.mesh.impl.PHalfEdge;
import org.vadere.util.geometry.mesh.inter.IMesh;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
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
import java.util.Optional;
import java.util.Set;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 */
public class UniformRefinementTriangulation<P extends IPoint> {
	private final Collection<VShape> boundary;
	private final VRectangle bbox;
	private final IEdgeLengthFunction lenFunc;
	private ITriangulation<P, PHalfEdge<P>, PFace<P>> pTriangulation;
	private Set<P> points;
	private IMesh<P, PHalfEdge<P>, PFace<P>> mesh;
	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulation.class);

	public UniformRefinementTriangulation(
			final VRectangle bound,
			final Collection<VShape> boundary,
			final IEdgeLengthFunction lenFunc,
			final IPointConstructor<P> pointConstructor) {
		this.pTriangulation = ITriangulation.createPTriangulation(IPointLocator.Type.DELAUNAY_TREE, bound, pointConstructor);
		this.mesh = pTriangulation.getMesh();
		this.boundary = boundary;
		this.lenFunc = lenFunc;
		this.bbox = bound;
		this.points = new HashSet<>();
	}

	public boolean isCompleted(final PHalfEdge<P> edge) {
		PFace<P> face = mesh.isBoundary(edge) ? mesh.getTwinFace(edge) : mesh.getFace(edge);
		VTriangle triangle = mesh.toTriangle(face);
		P end = mesh.getVertex(edge);
		P begin = mesh.getVertex(mesh.getPrev(edge));
		return !bbox.intersect(triangle) || boundary.stream().anyMatch(shape -> shape.contains(triangle.getBounds2D())) || begin.distance(end) <= lenFunc.apply(midPoint(edge));
	}

	public void compute() {
		pTriangulation.init();

		logger.info("start computation");
		LinkedList<PHalfEdge<P>> toRefineEdges = new LinkedList<>();

		for(PHalfEdge<P> edge : mesh.getEdgeIt(mesh.getBoundary())) {
			if(!isCompleted(edge) && !points.contains(mesh.getVertex(edge))) {
				toRefineEdges.add(edge);
			}
		}

		while (!toRefineEdges.isEmpty()) {
			PHalfEdge<P> edge = toRefineEdges.removeFirst();
			for(PHalfEdge<P> refinedHalfEdges : refine(edge)) {
				if(!isCompleted(refinedHalfEdges)) {
					toRefineEdges.addLast(refinedHalfEdges);
				}
			}
		}

		for(VShape shape : boundary) {
			// 1. find a triangle inside the boundary
			VPoint centroid = shape.getCentroid();

			Optional<PFace<P>> optFace = pTriangulation.locate(centroid.getX(), centroid.getY());

			if(optFace.isPresent()) {
				LinkedList<PFace<P>> candidates = new LinkedList<>();
				candidates.add(optFace.get());

				// 2. as long as there is a face which has a vertex inside the shape remove it
				while (!candidates.isEmpty()) {
					PFace<P> face = candidates.removeFirst();
					if(mesh.streamEdges(face).map(mesh::getVertex).anyMatch(shape::contains)) {
						mesh.streamFaces(face).forEach(f -> candidates.add(f));
						pTriangulation.removeFace(face, false);
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
		return pTriangulation.getEdges();
	}

	public Collection<PHalfEdge<P>> refine(final PHalfEdge<P> edge) {
		VPoint midPoint = midPoint(edge);
		P p = mesh.createVertex(midPoint.getX(), midPoint.getY());

		if(points.contains(p)) {
			return Collections.emptyList();
		}
		else {
			points.add(p);
			mesh.insert(p);
			return pTriangulation.splitEdge(p, edge);
		}
	}

	private VPoint midPoint(final PHalfEdge<P> edge) {
		VPoint p1 = new VPoint(mesh.getVertex(edge));
		VPoint p2 = new VPoint(mesh.getVertex(mesh.getPrev(edge)));
		return p2.add(p1).scalarMultiply(0.5);
	}

}
