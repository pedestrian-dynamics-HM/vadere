package org.vadere.util.delaunay;

import org.apache.commons.lang3.tuple.Triple;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

public class DAGElement<P extends VPoint, T extends VTriangle> {
	private Face<P> face;
	private Triple<P, P, P> vertices;
	private T triangle;

	public DAGElement(final Face<P> face, final Triple<P, P, P> vertices, final TriangleConstructor<P, T> triangleConstructor) {
		this.face = face;
		this.vertices = vertices;
		this.triangle = triangleConstructor.create(vertices.getLeft(), vertices.getMiddle(), vertices.getRight());
	}

	public Face<P> getFace() {
		return face;
	}

	public T getTriangle() {
		return triangle;
	}

	public Triple<P, P, P> getVertices() {
		return vertices;
	}
}
