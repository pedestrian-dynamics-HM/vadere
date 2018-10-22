package org.vadere.geometry.mesh.triangulation.triangulator;

import org.jetbrains.annotations.NotNull;
import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>A {@link SpaceFillingCurve} is a (linked-)list of nodes {@link SFCNode} in the order in which
 * the curve would travers these nodes. Each node contains a half-edge which refers to
 * a face.</p>
 *
 * @author Benedikt Zoennchen
 *
 * @param <P> generic type of the point
 * @param <V> generic type of the vertex
 * @param <E> generic type of the half-edge
 * @param <F> generic type of the face
 */
public class SpaceFillingCurve<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {
	private SFCNode<P, V, E, F> head;

	public SpaceFillingCurve(){
		head = null;
	}

	public void insertFirst(final SFCNode<P, V, E, F> node) {
		assert head == null;

		if(head != null) {
			throw new IllegalArgumentException("the space filling curve has already a first element");
		}

		head = node;
	}

	/**
	 * <p>Replaces the anchor element by two elements i.e. consecutive elements left->right.</p>
	 *
	 * @param left      the left element
	 * @param right     the right element
	 * @param anchor    the replaced element
	 */
	public void replace(
			@NotNull final SFCNode<P, V, E, F> left,
			@NotNull final SFCNode<P, V, E, F> right,
			@NotNull SFCNode<P, V, E, F> anchor) {
		assert asList().contains(anchor);
		insertNext(right, anchor);
		insertNext(left, anchor);
		remove(anchor);
	}

	/**
	 * <p>Removes an element from the SFC in O(1).</p>
	 *
	 * @param anchor the element which will be removed
	 */
	public void remove(@NotNull final SFCNode<P, V, E, F> anchor) {
		assert asList().contains(anchor);

		if(anchor == head) {
			head = head.next;
			head.prev = null;
		}
		else {
			anchor.prev.next = anchor.next;
			anchor.next.prev = anchor.prev;
		}

		anchor.destroy();
	}

	/**
	 * <p>Inserts a node after the anchor element in O(1).</p>
	 *
	 * @param node      the node
	 * @param anchor    the anchor element
	 */
	public void insertNext(@NotNull final SFCNode<P, V, E, F> node, @NotNull SFCNode<P, V, E, F> anchor) {
		assert asList().contains(anchor);

		SFCNode<P, V, E, F> anchorNext = anchor.next;
		anchor.next = node;
		node.prev = anchor;
		node.next = anchorNext;

		// tail!
		if(anchorNext != null) {
			anchorNext.prev = node;
		}

	}

	/**
	 * Returns the SFC as ordered <tt>List</tt>.
	 * @return the whole SFC as in an ordered list
	 */
	public List<SFCNode<P, V, E, F>> asList() {
		List<SFCNode<P, V, E, F>> list;
		if(head == null) {
			list = Collections.EMPTY_LIST;
		}
		else {
			list = new ArrayList<>();
			SFCNode<P, V, E, F> node = head;
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
