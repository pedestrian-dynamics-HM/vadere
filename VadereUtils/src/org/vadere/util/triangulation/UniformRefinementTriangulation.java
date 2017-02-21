package org.vadere.util.triangulation;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.FixPointGenerator;
import org.vadere.util.geometry.data.Face;
import org.vadere.util.geometry.data.HalfEdge;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 */
public class UniformRefinementTriangulation<P extends IPoint> {
	private final Collection<VShape> boundary;
	private final VRectangle bbox;
	private final IEdgeLengthFunction lenFunc;
	private final Set<HalfEdge<P>> refinedEdges;
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

	public boolean isCompleted(final HalfEdge<P> edge) {
		VTriangle triangle = edge.isBoundary() ? edge.getTwin().getFace().toTriangle() : edge.getFace().toTriangle();
		//System.out.println(bbox.distance(triangle.midPoint()));
		return !bbox.intersect(triangle) || boundary.stream().anyMatch(shape -> shape.contains(triangle.getBounds2D())) || edge.getEnd().distance(edge.getPrevious().getEnd()) <= lenFunc.apply(midPoint(edge));
	}

	public void compute() {
		logger.info("start computation");
		int count = 0;
		LinkedList<HalfEdge<P>> toRefineEdges = new LinkedList<>();
		List<HalfEdge<P>> edges = triangulation.superTriangle.getEdges();

		for(HalfEdge<P> edge : edges) {
			if(!isCompleted(edge) && !points.contains(edge.getEnd())) {
				toRefineEdges.add(edge);
			}
		}

		while (!toRefineEdges.isEmpty()) {
			HalfEdge<P> edge = toRefineEdges.removeFirst();
			count++;
			for(HalfEdge<P> refinedHalfEdges : refine(edge)) {
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
				boolean changed = true;
				HalfEdge<P> startEdge = face.getEdge();
				HalfEdge<P> edge = null;

				while (!startEdge.equals(edge)) {
					if (edge == null) {
						edge = startEdge;
					}

					boolean eqNext = edge.getTwin().getFace().equals(edge.getNext().getTwin().getFace());
					boolean eqPrev = edge.getTwin().getFace().equals(edge.getPrevious().getTwin().getFace());

					if (eqNext || eqPrev) {
						HalfEdge<P> h0 = eqNext ? edge : edge.getPrevious();
						if (shape.intersects(h0.toLine()) || shape.intersects(h0.getNext().toLine())) {
							// change the face
							HalfEdge<P> middleEdge = h0.getTwin().getNext();
							middleEdge.setFace(face);

							// build connectivity
							h0.getPrevious().setNext(middleEdge);
							h0.getNext().getNext().setPrevious(middleEdge);

							// change the edge of the face since we might destroy it
							face.setEdge(h0.getNext().getNext());
							edge = h0.getNext().getNext();
							startEdge = middleEdge;

							// destroy
							h0.getNext().getTwin().destroy();
							h0.getNext().destroy();
							h0.getFace().destroy();

							h0.getTwin().destroy();
							h0.destroy();

						} else {
							edge = edge.getNext();
						}
					} else if (shape.intersects(edge.toLine())) {
						HalfEdge<P> twin = edge.getTwin();
						HalfEdge<P> destroyEdge = edge;

						// change the face
						twin.getNext().setFace(face);
						twin.getPrevious().setFace(face);

						// build connectivity
						edge.getPrevious().setNext(twin.getNext());
						edge.getNext().setPrevious(twin.getPrevious());
						startEdge = twin.getPrevious();

						// change the edge of the face since we might destroy it
						face.setEdge(edge.getNext());
						edge = edge.getNext();

						// destroy
						twin.getFace().destroy();
						destroyEdge.destroy();
						twin.destroy();
					} else {
						edge = edge.getNext();
					}
				}
			}
		}
		logger.info("end computation");
	}

	public Set<VLine> getEdges() {
		return triangulation.getEdges();
	}

	public Collection<HalfEdge<P>> refine(final HalfEdge<P> edge) {
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

	private boolean flipEdgeCrossBoundary(final HalfEdge<P> edge) {
		P p1 = edge.getNext().getEnd();
		P p2 = edge.getTwin().getNext().getEnd();

		for(VShape shape : boundary) {
			if(shape.intersects(new VLine(new VPoint(p1), new VPoint(p2)))) {
				return true;
			}
		}

		return false;
	}

	private VPoint midPoint(final HalfEdge<P> edge) {
		VPoint p1 = new VPoint(edge.getEnd());
		VPoint p2 = new VPoint(edge.getPrevious().getEnd());
		return p2.add(p1).scalarMultiply(0.5);
	}

}
