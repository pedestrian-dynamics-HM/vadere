package org.vadere.util.data.aabb;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.LinkedList;

/**
 * Not jet finished!
 *
 * @author Benedikt Zoennchen
 * @param <D>
 */
public class AABBTree<D> {

	private AABBNode<D> root;
	private int capacity = 16;
	private int nElements = 0;


	public LinkedList<AABBNode<D>> query(@NotNull final VRectangle aabb) {
		LinkedList<AABBNode<D>> intersections = new LinkedList<>();
		LinkedList<AABBNode<D>> toProcess = new LinkedList<>();

		toProcess.add(root);

		while (!toProcess.isEmpty()) {
			AABBNode<D> node = toProcess.poll();

			if(intersects(node.getAabb(), aabb)) {
				if(node.isLeaf()) {
					intersections.add(node);
				}
				else {
					toProcess.add(node.getLeft());
					toProcess.add(node.getRight());
				}
			}
		}

		return intersections;
	}

	public void insert(@NotNull final AABBNode<D> node) {
		if(root == null) {
			root = node;
		}
		else {
			// find the heuristicly best sibling
			AABBNode<D> currentNode = root;
			while (!currentNode.isLeaf()) {
				AABBNode<D> left = currentNode.getLeft();
				AABBNode<D> right = currentNode.getRight();
				double area = cost(currentNode.getAabb());

				double combinedArea = cost(combine(currentNode.getAabb(), node.getAabb()));
				double cominbedCost = 2 * combinedArea;

				// min cost further down
				double inheritanceCost = 2 * (combinedArea - area);

				double leftCost;
				if(left.isLeaf()) {
					leftCost = cost(combine(node.getAabb(), left.getAabb())) + inheritanceCost;
				} else {
					double newArea = cost(combine(node.getAabb(), left.getAabb()));
					double oldArea = cost(left.getAabb());
					leftCost = (newArea - oldArea) + inheritanceCost;
				}

				double rightCost;
				if(left.isLeaf()) {
					rightCost = cost(combine(node.getAabb(), right.getAabb())) + inheritanceCost;
				} else {
					double newArea = cost(combine(node.getAabb(), right.getAabb()));
					double oldArea = cost(right.getAabb());
					rightCost = (newArea - oldArea) + inheritanceCost;
				}

				if(combinedArea < leftCost && combinedArea < rightCost) {
					break;
				}

				if(leftCost < rightCost) {
					currentNode = left;
				}
				else {
					currentNode = right;
				}
			}

			AABBNode<D> oldParent = currentNode.getParent();
			AABBNode<D> newParent = new AABBNode<>();
			newParent.setParent(oldParent);
			newParent.setAabb(combine(node.getAabb(), currentNode.getAabb()));
			newParent.setHeight(currentNode.getHeight() + 1);

			// old parent is not the root
			if(oldParent != null) {

			}

		}
	}

	private boolean intersects(@NotNull final VRectangle rect1, @NotNull final VRectangle rect2) {
		return rect1.intersects(rect2.getX(), rect2.getY(), rect2.getWidth(), rect2.getHeight());
	}

	private VRectangle combine(@NotNull final VRectangle rect1, @NotNull final VRectangle rect2) {
		double xmin = Math.min(rect1.getMinX(), rect2.getMinX());
		double ymin = Math.min(rect1.getMinY(), rect2.getMinY());
		double xmax = Math.max(rect1.getMaxX(), rect2.getMaxX());
		double ymax = Math.max(rect1.getMaxY(), rect2.getMaxY());
		return new VRectangle(xmin, ymin, xmax - xmin, ymax-ymin);
	}

	private double cost(@NotNull VRectangle rectangle) {
		return rectangle.getArea();
	}

}
