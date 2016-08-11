package org.vadere.util.voronoi;

import java.util.List;

import org.vadere.util.geometry.shapes.VPoint;

/**
 * Double-Connected Edge List according to the data structure described in the
 * book Computational Geometry by Mark de Berg et al. (2008) pages 29ff.
 */
public class HalfEdge {
	private VPoint origin;

	private HalfEdge next;
	private HalfEdge previous;
	private HalfEdge twin;

	private Face face;

	HalfEdge(Face face) {
		this.face = face;
	}

	public HalfEdge getNext() {
		return next;
	}

	public void setNext(HalfEdge next) {
		if (this.next != null) {
			// throw new IllegalStateException();
		}
		this.next = next;
	}

	public HalfEdge getPrevious() {
		return previous;
	}

	public void setPrevious(HalfEdge previous) {
		if (this.previous != null) {
			// throw new IllegalStateException();
		}
		this.previous = previous;
	}

	public Face getFace() {
		return face;
	}

	public void setFace(Face face) {
		this.face = face;
	}

	HalfEdge getTwin() {
		return twin;
	}

	void setTwin(HalfEdge twin) {
		this.twin = twin;
	}

	public VPoint getOrigin() {
		return origin;
	}

	void setOrigin(VPoint origin) {
		this.origin = origin;
	}

	// static helper methods...

	static void setTwins(HalfEdge a, HalfEdge b) {
		a.setTwin(b);
		b.setTwin(a);
	}

	static void setInSuccession(HalfEdge a, HalfEdge b) {
		a.setNext(b);
		b.setPrevious(a);
	}

	// static methods handling Edges after Events...

	static void handleSiteEventEdges(List<Face> faces, BeachLineLeaf newLeaf,
			BeachLineLeaf arcAboveLeaf, boolean siteOnHorizontalLine) {

		if (!siteOnHorizontalLine) {
			BeachLineInternal lowerNode = newLeaf.getParent();
			BeachLineInternal upperNode = lowerNode.getParent();

			HalfEdge halfEdgeLeft = newLeaf.getFace().getOuterComponent();

			Face upperFace = arcAboveLeaf.getFace();
			HalfEdge halfEdgeRight = new HalfEdge(upperFace);;

			if (upperFace.getOuterComponent() == null) {
				upperFace.setOuterComponent(halfEdgeRight);
			}

			setTwins(halfEdgeLeft, halfEdgeRight);

			upperNode.setHalfEdge(halfEdgeLeft);
			lowerNode.setHalfEdge(halfEdgeRight);
		}
		// degenerated case
		else {
			BeachLineInternal upperNode = newLeaf.getParent();

			HalfEdge halfEdgeLeft = newLeaf.getFace().getOuterComponent();

			Face upperFace = arcAboveLeaf.getFace();
			HalfEdge halfEdgeRight = new HalfEdge(upperFace);;

			if (upperFace.getOuterComponent() == null) {
				upperFace.setOuterComponent(halfEdgeRight);
			}

			setTwins(halfEdgeLeft, halfEdgeRight);

			upperNode.setHalfEdge(halfEdgeLeft);
		}
	}

	static HalfEdge handleCircleEventEdges(BeachLineInternal leftNode,
			BeachLineInternal rightNode, VPoint vertex) {

		HalfEdge rightEdge = rightNode.getHalfEgde();
		HalfEdge leftEdge = leftNode.getHalfEgde();
		HalfEdge rightEdgeTwin = rightEdge.getTwin();
		HalfEdge leftEdgeTwin = leftEdge.getTwin();

		HalfEdge newLeftEdge = new HalfEdge(leftEdgeTwin.getFace());
		HalfEdge newRightEdge = new HalfEdge(rightEdge.getFace());
		setTwins(newLeftEdge, newRightEdge);

		rightEdgeTwin.setOrigin(vertex);
		leftEdgeTwin.setOrigin(vertex);
		newRightEdge.setOrigin(vertex);

		setInSuccession(leftEdge, rightEdgeTwin);
		setInSuccession(rightEdge, newRightEdge);
		setInSuccession(newLeftEdge, leftEdgeTwin);

		return newRightEdge;
	}
}
