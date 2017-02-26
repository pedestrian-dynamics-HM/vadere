package org.vadere.util.triangulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.Face;
import org.vadere.util.geometry.mesh.PHalfEdge;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.triangulation.adaptive.IEdgeLengthFunction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
	private final Set<PHalfEdge<P>> refinedEdges;
	private int splitCount;
	private IncrementalTriangulation<P> triangulation;
	private IPointConstructor<P> pointConstructor;
	private Set<P> points;
	private static final Logger logger = LogManager.getLogger(UniformRefinementTriangulation.class);

	public UniformRefinementTriangulation(
			final double minX,
			final double minY,
			final double width,
			final double height,
			final IPointConstructor<P> pointConstructor,
			final Collection<VShape> boundary,
			final IEdgeLengthFunction lenFunc) {
		//Predicate<HalfEdge<P>> isIllegal = edge -> IncrementalTriangulation.isIllegalEdge(edge) && !flipEdgeCrossBoundary(edge);
		this.triangulation = new IncrementalTriangulation<>(minX, minY, width, height, pointConstructor);
		this.boundary = boundary;
		this.pointConstructor = pointConstructor;
		this.lenFunc = lenFunc;
		this.bbox = new VRectangle(minX, minY, width, height);
		this.refinedEdges = new HashSet<>();
		this.points = new HashSet<>();
		this.splitCount = 0;
	}

	public boolean isCompleted(final PHalfEdge<P> edge) {
		VTriangle triangle = edge.isBoundary() ? edge.getTwin().getFace().toTriangle() : edge.getFace().toTriangle();
		//System.out.println(bbox.distance(triangle.midPoint()));
		return !bbox.intersect(triangle) || boundary.stream().anyMatch(shape -> shape.contains(triangle.getBounds2D())) || edge.getEnd().distance(edge.getPrevious().getEnd()) <= lenFunc.apply(midPoint(edge));
	}

	public void compute() {
		logger.info("start computation");
		int count = 0;
		LinkedList<PHalfEdge<P>> toRefineEdges = new LinkedList<>();
		List<PHalfEdge<P>> edges = triangulation.superTriangle.getEdges();

		for(PHalfEdge<P> edge : edges) {
			if(!isCompleted(edge) && !points.contains(edge.getEnd())) {
				toRefineEdges.add(edge);
			}
		}

		while (!toRefineEdges.isEmpty()) {
			PHalfEdge<P> edge = toRefineEdges.removeFirst();
			count++;
			for(PHalfEdge<P> refinedHalfEdges : refine(edge)) {
				if(!isCompleted(refinedHalfEdges)) {
					toRefineEdges.addLast(refinedHalfEdges);
				}
			}
		}

		for(VShape shape : boundary) {
			count = 0;
			// 1. find a trinagle inside the boundary
			Face<P> face = triangulation.locate(shape.getCentroid());
			if(face != null) {
				// 2. build a border-face: delete edges as long as they are inside the boundary
				//face.toBorder();
				PHalfEdge<P> beginEdge = face.getEdge();
				PHalfEdge<P> currentEdge = null;

				while (!beginEdge.equals(currentEdge) && count < 7941) {
					if (currentEdge == null) {
						currentEdge = beginEdge;
					}

					if(currentEdge.getFace().equals(currentEdge.getTwin().getFace())) {
						System.out.println("special case");
					}

					PHalfEdge<P> candidate = currentEdge;
					LinkedList<PHalfEdge<P>> toRemove = new LinkedList<>();
					toRemove.addLast(candidate);
					while (candidate.hasSameTwinFace(candidate.getNext())) {
						toRemove.addLast(candidate.getNext());
						candidate = candidate.getNext();
					}

					candidate = currentEdge;
					while(candidate.hasSameTwinFace(candidate.getPrevious())) {
						toRemove.addFirst(candidate.getPrevious());
						candidate = candidate.getPrevious();
					}

					System.out.println("toRemove:" + toRemove.size());

					PHalfEdge<P> next = toRemove.peekLast().getNext();
					PHalfEdge<P> previous = toRemove.peekFirst().getPrevious();

					LinkedList<PHalfEdge<P>> toUpdate = new LinkedList<>();
					PHalfEdge<P> start = toRemove.peekFirst().getTwin().getNext();
					PHalfEdge<P> end = toRemove.peekLast().getTwin();

					while (!start.equals(end)) {
						toUpdate.addLast(start);
						start = start.getNext();
					}

					System.out.println("toUpdate:" + toUpdate.size());

					if((toUpdate.size() + toRemove.size()) != 3) {
						System.out.println("wtf");
					}

					if(!toUpdate.isEmpty()) {
						toUpdate.peekFirst().getFace().destroy();
						for(PHalfEdge<P> halfEdge : toUpdate) {
							halfEdge.setFace(face);
						}
					}

					currentEdge = toRemove.peekLast().getNext();
					face.setEdge(currentEdge);
					for(PHalfEdge<P> halfEdge : toRemove) {
						halfEdge.getTwin().destroy();
						halfEdge.destroy();
					}

					next.setPrevious(toUpdate.peekLast());
					previous.setNext(toUpdate.peekFirst());

					count++;
					System.out.println(count);
				}
			}
		}
		logger.info("end computation");
	}

	public Set<VLine> getEdges() {
		return triangulation.getEdges();
	}

	public Collection<PHalfEdge<P>> refine(final PHalfEdge<P> edge) {
		VPoint midPoint = midPoint(edge);
		P p = pointConstructor.create(midPoint.getX(), midPoint.getY());

		if(points.contains(p)) {
			return Collections.emptyList();
		}
		else {
			points.add(p);
			splitCount++;
			return triangulation.splitEdgeDB(p, edge);
		}
	}

	private boolean flipEdgeCrossBoundary(final PHalfEdge<P> edge) {
		P p1 = edge.getNext().getEnd();
		P p2 = edge.getTwin().getNext().getEnd();

		for(VShape shape : boundary) {
			if(shape.intersects(new VLine(new VPoint(p1), new VPoint(p2)))) {
				return true;
			}
		}

		return false;
	}

	private VPoint midPoint(final PHalfEdge<P> edge) {
		VPoint p1 = new VPoint(edge.getEnd());
		VPoint p2 = new VPoint(edge.getPrevious().getEnd());
		return p2.add(p1).scalarMultiply(0.5);
	}

}
