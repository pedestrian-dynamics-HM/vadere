package org.vadere.util.geometry.data;

import org.apache.commons.collections.IteratorUtils;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HalfEdge<P extends IPoint> implements Iterable<HalfEdge<P>> {

	/**
	 * point at the end of the half edge.
	 */
	private P end;

	/**
	 * next half-edge around the face.
	 */
	private HalfEdge<P> next;

	/**
	 * previous half-edge around the face.
	 */
	private HalfEdge<P> previous;

	/**
	 * oppositely oriented adjacnet half-edge. If the face is a the boundary
	 * there is no twin.
	 */
	private HalfEdge<P> twin;

	/**
	 * the face the half-edge borders.
	 */
	private Face<P> face;


	public HalfEdge (@NotNull final P end, @NotNull final Face<P> face) {
		this.end = end;
		this.face = face;
	}

	public Face getFace() {
		return face;
	}

	public void setFace(final Face<P> face) {
		this.face = face;
	}

	public P getEnd() {
		return end;
	}

	public boolean hasNext() {
		return next != null;
	}

	public boolean hasTwin() {
		return twin != null;
	}

	public HalfEdge<P> getNext() {
		return next;
	}

	public HalfEdge<P> getPrevious() {
		return previous;
	}

	public HalfEdge<P> getTwin() {
		return twin;
	}

	/**
	 * Deletes the vertex i.e. end point of this halfedge from the geometry by deleting all halfedges
	 * connected to this vertex.
	 */
	public void deleteVertex() {
		// 1. gather all edges
		List<HalfEdge<P>> edges = IteratorUtils.toList(this.iterator());

		// 2. delete all edges of connected to the end point
		edges.stream().filter(HalfEdge::isValid).forEach(edge -> edge.deleteEdge());
	}

	/**
	 * Deletes this edge (and its twin) from the geometry. This operation requires O(n) where
	 * n is the number of edges inside a face. It may delete an vertex if it has degree = 2.
	 * Furthermore, faces may be merged. If a face will become invalid it will be the face of this
	 * edge.
	 *
	 * @return true if the operation deletes a vertex i.e. an end point is no longer part of the geometry, false otherwise.
	 */
	public boolean deleteEdge() {

		boolean deleteVertex = false;

		// the edge is inside another face.
		if(!getFace().isBorder() && !getTwin().getFace().isBorder()) {
			// 1. remove one of the 2 faces. We deleteEdge the face of this edge, the twin face survives.
			for(HalfEdge<P> halfEdge : this.face) {
				halfEdge.setFace(getTwin().getFace());
			}

			// 2. Delete the edge and its twin be rearranging pointers
			HalfEdge<P> xy = this;
			HalfEdge<P> yx = getTwin();

			HalfEdge<P> yz = xy.getNext();
			HalfEdge<P> wy = yx.getPrevious();
			wy.setNext(yz);

			HalfEdge<P> ux = xy.getPrevious();
			HalfEdge<P> xt = yx.getNext();
			ux.setNext(xt);

			// 3. update the edge of the survived face since it might be the twin.
			getTwin().getFace().setEdge(ux);
		} // the edge is on the border, therefore we remove the whole non-border face if this face does only consist of <= 3 edges before the deletion.
		else {
			Face<P> borderFace = getFace().isBorder() ? getFace() : getTwin().getFace();
			Face<P> nonBorderFace = getFace().isBorder() ? getTwin().getFace() : getFace();

			HalfEdge<P> borderHe = getFace().isBorder() ? this : getTwin();
			HalfEdge<P> nonBorderHe = getFace().isBorder() ? getTwin() : this;

			// nonBorder-Face is not a triangle
			if(!nonBorderHe.getNext().getTwin().getFace().isBorder() && !nonBorderHe.getPrevious().getTwin().getFace().isBorder()) {
				for(HalfEdge<P> halfEdge : nonBorderFace) {
					halfEdge.setFace(borderFace);
				}

				// since the face may has this edge as pointer which will be invalid
				borderFace.setEdge(borderHe.getNext());
				//nonBorderFace.setEdge(nonBorderHe.getNext());

				nonBorderHe.getPrevious().setNext(borderHe.getNext());
				nonBorderHe.getNext().setPrevious(borderHe.getPrevious());
			}
			// special case1: there is no possibility to delete this edge without deleting the vertex, since the vertex has degree 2.
			else if(!borderHe.equals(borderHe.getNext().getNext().getNext())) {
				borderFace.setEdge(borderHe.getNext());
				nonBorderFace.setEdge(nonBorderHe.getNext());

				borderHe.getPrevious().setNext(borderHe.getNext());
				nonBorderHe.getPrevious().setNext(nonBorderHe.getNext());
				deleteVertex = true;
			}
			// special case2: inner face and outer face is a triangle => there is only 1 inner face.
			// if we delete the edge there is no face in the geometry. Therefore we delete the whole triangle.
			else {
				HalfEdge<P> y = getNext();
				HalfEdge<P> z = y.getNext();

				// delete pointers for the GC
				y.getTwin().destroy();
				y.destroy();
				z.getTwin().destroy();
				z.destroy();
				deleteVertex = true;
			}
		}

		// delete pointers for the GC
		getTwin().destroy();
		destroy();
		return deleteVertex;
	}

	/**
	 * removes the cyclic pointer structure such that the GC can delete these objects.
	 */
	private void destroy() {
		setNext(null);
		setPrevious(null);
		setTwin(null);
		setFace(null);
	}

	public boolean isValid() {
		return twin != null && next != null && previous != null && face != null;
	}

	public void setTwin(final HalfEdge twin) {
		this.twin = twin;
		if(twin != null && twin.getTwin() != this) {
			twin.setTwin(this);
		}
	}

	public void setPrevious(final HalfEdge<P> previous) {
		this.previous = previous;
		if(previous != null && previous.getNext() != this) {
			previous.setNext(this);
		}
	}

	public void setNext(final HalfEdge<P> next) {
		this.next = next;
		if(next != null && next.getPrevious() != this) {
			next.setPrevious(this);
		}
	}

	public VLine toLine() {
		return new VLine((VPoint) this.getPrevious().getEnd(), (VPoint) this.getEnd());
	}

	public Iterator<HalfEdge<P>> incidentVertexIterator() {
		return new NeighbourIterator();
	}

	public Iterator<Face<P>> incidentFaceIterator() { return new NeighbourFaceIterator(); }



	public List<HalfEdge<P>> getIncidentPoints() {
		List<HalfEdge<P>> incidentPoints = new ArrayList<>();
		Iterator<HalfEdge<P>> iterator = incidentVertexIterator();

		while (iterator.hasNext()) {
			incidentPoints.add(iterator.next());
		}

		return incidentPoints;
	}

	@Override
	public String toString() {
		return getEnd().toString();
	}

	@Override
	public Iterator<HalfEdge<P>> iterator() {
		return incidentVertexIterator();
	}

	/**
	 * This iterator assumes that the this edge is completely surrounded by faces.
	 */
	private class NeighbourFaceIterator implements Iterator<Face<P>> {
		private NeighbourIterator neighbourIterator = new NeighbourIterator();

		private NeighbourFaceIterator() {
			// such that no duplicated faces returned
			if(neighbourIterator.hasNext()) {
				neighbourIterator.next();
			}
		}

		@Override
		public boolean hasNext() {
			return neighbourIterator.hasNext();
		}

		@Override
		public Face<P> next() {
			return neighbourIterator.next().getFace();
		}
	}

	/**
	 * This iterator assumes that the this edge is completely surrounded by faces.
	 */
	private class NeighbourIterator implements Iterator<HalfEdge<P>> {

		private HalfEdge<P> current = HalfEdge.this.getNext();
		private boolean first = true;

		@Override
		public boolean hasNext() {
			return (first || current != HalfEdge.this.getNext());
		}

		@Override
		public HalfEdge<P> next() {
			HalfEdge<P> result = current;
			current = result.getTwin().getNext();
			first = false;
			return result;
		}
	}

	/*
	 * A half-edge is defined by its end vertex and its face. In a geometry there can not be more than
	 * one half-edge part of face and ending at end.
	 */

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		HalfEdge<?> halfEdge = (HalfEdge<?>) o;

		if (!end.equals(halfEdge.end)) return false;
		return face != null ? face.equals(halfEdge.face) : halfEdge.face == null;
	}

	@Override
	public int hashCode() {
		int result = end.hashCode();
		result = 31 * result + (face != null ? face.hashCode() : 0);
		return result;
	}
}
