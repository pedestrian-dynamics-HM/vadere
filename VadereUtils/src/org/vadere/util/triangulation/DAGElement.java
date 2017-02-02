package org.vadere.util.triangulation;

import org.apache.commons.lang3.tuple.Triple;
import org.vadere.util.geometry.data.Face;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

public class DAGElement<P extends IPoint> {
	private Face<P> face;
	private Triple<P, P, P> vertices;
	private VTriangle triangle;

	public DAGElement(final Face<P> face, final Triple<P, P, P> vertices) {
		this.face = face;
		this.vertices = vertices;
		VPoint p1 = new VPoint(vertices.getLeft().getX(), vertices.getLeft().getY());
		VPoint p2 = new VPoint(vertices.getMiddle().getX(), vertices.getMiddle().getY());
		VPoint p3 = new VPoint(vertices.getRight().getX(), vertices.getRight().getY());
		this.triangle = new VTriangle(p1, p2, p3);
	}

	public Face<P> getFace() {
		return face;
	}

	public VTriangle getTriangle() {
		return triangle;
	}

	public Triple<P, P, P> getVertices() {
		return vertices;
	}

	@Override
	public String toString() {
		return triangle.toString();
	}
}
