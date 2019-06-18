package org.vadere.meshing.mesh.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.data.Node;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GenEar<V extends IVertex, E extends IHalfEdge, F extends IFace> implements Comparable<GenEar<V, E, F>>{

	private final List<E> edges;
	private double power;

	public GenEar(@NotNull final E e1, @NotNull final E e2, @NotNull final E e3, final double power) {
		this.edges = new ArrayList<>(3);
		this.edges.add(e1);
		this.edges.add(e2);
		this.edges.add(e3);
		this.power = power;
	}

	public E getLast() {
		return this.edges.get(2);
	}

	public E getFirst() {
		return this.edges.get(0);
	}

	public void setLast(@NotNull final E e) {
		this.edges.set(2, e);
	}

	public void setFirst(@NotNull final E e) {
		this.edges.set(0, e);
	}

	public void setMiddle(@NotNull final E e) {
		this.edges.set(1, e);
	}

	private double getPower() {
		return power;
	}

	public void setPower(final double power) {
		this.power = power;
	}

	public List<E> getEdges() {
		return edges;
	}

	@Override
	public int compareTo(@NotNull final GenEar<V, E, F> o) {
		return Double.compare(getPower(), o.getPower());
	}

	public static class EarNodeComparator<
			V extends IVertex,
			E extends IHalfEdge,
			F extends IFace> implements Comparator<Node<GenEar<V, E, F>>> {

		@Override
		public int compare(@NotNull final Node<GenEar<V, E, F>> o1, @NotNull final Node<GenEar<V, E, F>> o2) {
			return Double.compare(o1.getElement().getPower(), o2.getElement().getPower());
		}
	}
}
