package org.vadere.util.data.aabb;

import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

public class AABBNode<D> {
	private AABBNode parent;
	private VRectangle aabb;

	private AABBNode left;
	private AABBNode right;

	private D data;

	int height;

	public boolean isLeaf() {
		return left == null && right == null;
	}

	public VRectangle getAabb() {
		return aabb;
	}

	public AABBNode() {

	}

	public int getHeight() {
		return height;
	}

	public void setAabb(VRectangle aabb) {
		this.aabb = aabb;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setParent(AABBNode parent) {
		this.parent = parent;
	}

	public void setLeft(AABBNode left) {
		this.left = left;
	}

	public void setRight(AABBNode right) {
		this.right = right;
	}

	public void setData(D data) {
		this.data = data;
	}

	public AABBNode getParent() {
		return parent;
	}

	public AABBNode getLeft() {
		return left;
	}

	public AABBNode getRight() {
		return right;
	}

	public D getData() {
		return data;
	}
}
