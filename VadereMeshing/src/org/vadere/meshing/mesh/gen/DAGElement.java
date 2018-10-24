package org.vadere.meshing.mesh.gen;

import org.apache.commons.lang3.tuple.Triple;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;

import java.util.List;

public class DAGElement<P extends IPoint, F extends IFace<P>> {
	private F face;
	private Triple<P, P, P> vertices;
	private VTriangle triangle;

	public DAGElement(final F face, List<P> points) {
		P p1 = points.get(0);
		P p2 = points.get(1);
		P p3 = points.get(2);

		this.face = face;
		this.vertices = Triple.of(p1, p2, p3);
		this.triangle = new VTriangle(new VPoint(p1), new VPoint(p2), new VPoint(p3));
	}

	public DAGElement(final F face, final Triple<P, P, P> vertices) {
		this.face = face;
		this.vertices = vertices;
		VPoint p1 = new VPoint(vertices.getLeft());
		VPoint p2 = new VPoint(vertices.getMiddle());
		VPoint p3 = new VPoint(vertices.getRight());
		this.triangle = new VTriangle(p1, p2, p3);
	}

	public F getFace() {
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
