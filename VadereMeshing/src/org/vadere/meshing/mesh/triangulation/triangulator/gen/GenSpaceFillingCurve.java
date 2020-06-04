package org.vadere.meshing.mesh.triangulation.triangulator.gen;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>A {@link GenSpaceFillingCurve} is a (linked-)list of nodes {@link SFCNode} in the order in which
 * the curve would travers these nodes. Each node contains a half-edge which refers to
 * a face.</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <V> the type of the vertices
 * @param <E> the type of the half-edges
 * @param <F> the type of the faces
 */
public class GenSpaceFillingCurve<V extends IVertex, E extends IHalfEdge, F extends IFace> {
	private SFCNode<V, E, F> head;
	private Map<E, SFCNode<V, E, F>> edgeToNode;

	public GenSpaceFillingCurve(){
		head = null;
		edgeToNode = new HashMap<>();
	}

	public void insertFirst(final SFCNode<V, E, F> node) {
		assert head == null;

		if(head != null) {
			throw new IllegalArgumentException("the space filling curve has already a first element");
		}

		head = node;
		edgeToNode.put(head.getEdge(), head);
	}

	/**
	 * <p>Replaces the anchor element by two elements i.e. consecutive elements left followed by right.</p>
	 *
	 * @param left      the left element
	 * @param right     the right element
	 * @param anchor    the replaced element
	 */
	public void replace(
			@NotNull final SFCNode<V, E, F> left,
			@NotNull final SFCNode<V, E, F> right,
			@NotNull SFCNode<V, E, F> anchor) {
		assert asList().contains(anchor);
		insertNext(right, anchor);
		insertNext(left, anchor);
		remove(anchor);
	}

	/**
	 * <p>Replaces the anchor element by two elements i.e. consecutive elements left followed by right.</p>
	 *
	 * @param left      the left element
	 * @param middle    the middle element
	 * @param right     the right element
	 * @param anchor    the replaced element
	 */
	public void replace(
			@NotNull final SFCNode<V, E, F> left,
			@NotNull final SFCNode<V, E, F> middle,
			@NotNull final SFCNode<V, E, F> right,
			@NotNull SFCNode<V, E, F> anchor) {
		assert asList().contains(anchor);
		insertNext(right, anchor);
		insertNext(middle, anchor);
		insertNext(left, anchor);
		remove(anchor);
	}

	public SFCNode<V, E, F> getNode(@NotNull final E edge) {
		return edgeToNode.get(edge);
	}


	public SFCNode<V, E, F> getNode(@NotNull final F face) {
		return edgeToNode.get(face);
	}

	/**
	 * <p>Removes an element from the SFC in O(1).</p>
	 *
	 * @param anchor the element which will be removed
	 */
	public void remove(@NotNull final SFCNode<V, E, F> anchor) {
		assert asList().contains(anchor);

		if(anchor == head) {
			head = head.next;
			head.prev = null;
		}
		else {
			anchor.prev.next = anchor.next;
			anchor.next.prev = anchor.prev;
		}

		// maybe we already replaced the anchor in edgeToNode
		if(edgeToNode.get(anchor.getEdge()).equals(anchor)) {
			edgeToNode.remove(anchor.getEdge());
		}
		anchor.destroy();
	}

	/**
	 * <p>Inserts a node after the anchor element in O(1).</p>
	 *
	 * @param node      the node
	 * @param anchor    the anchor element
	 */
	public void insertNext(@NotNull final SFCNode<V, E, F> node, @NotNull SFCNode<V, E, F> anchor) {
		assert asList().contains(anchor);

		SFCNode<V, E, F> anchorNext = anchor.next;
		anchor.next = node;
		node.prev = anchor;
		node.next = anchorNext;

		// tail!
		if(anchorNext != null) {
			anchorNext.prev = node;
		}

		edgeToNode.put(node.getEdge(), node);
	}

	/**
	 * Returns the SFC as ordered <tt>List</tt>.
	 * @return the whole SFC as in an ordered list
	 */
	public List<SFCNode<V, E, F>> asList() {
		List<SFCNode<V, E, F>> list;
		if(head == null) {
			list = Collections.EMPTY_LIST;
		}
		else {
			list = new ArrayList<>();
			SFCNode<V, E, F> node = head;
			while (node != null) {
				list.add(node);
				node = node.next;
			}
		}
		return list;
	}

	@Override
	public String toString() {
		return asList().toString();
	}
}
