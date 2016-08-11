package org.vadere.util.voronoi;

import java.util.LinkedList;
import java.util.List;

import org.vadere.util.geometry.shapes.VPoint;

public class BeachLine {
	private BeachLineNode root = null;
	private final RectangleLimits limits;

	public BeachLine(RectangleLimits limits) {
		this.limits = limits;
	}

	boolean isEmpty() {
		return root == null;
	}

	void setRoot(int id, VPoint site, List<Face> faces) {
		if (root == null) {
			Face f = new Face(id, site, limits);
			root = new BeachLineLeaf(site, f);
			faces.add(f);
		} else {
			throw new IllegalArgumentException();
		}
	}

	BeachLineLeaf getArcAboveSite(VPoint site) {
		BeachLineNode current = root;
		double yCoordinate = site.y;
		double xCoordinate = site.x;

		while (current.getClass() == BeachLineInternal.class) {
			BeachLineInternal internal = (BeachLineInternal) current;

			if (internal.getBreakPoint(yCoordinate) < xCoordinate) {
				current = internal.getRightChild();
			} else {
				current = internal.getLeftChild();
			}
		}

		return (BeachLineLeaf) current;
	}

	BeachLineLeaf addSite(int id, VPoint site, BeachLineLeaf arc,
			List<Face> faces, boolean siteOnHorizontalLine) {

		BeachLineInternal parent;
		BeachLineLeaf centerLeaf;
		BeachLineInternal upperNode;

		if (!siteOnHorizontalLine) {
			parent = arc.getParent();

			BeachLineLeaf leftLeaf = new BeachLineLeaf(arc.getSite(),
					arc.getFace());
			BeachLineLeaf rightLeaf = new BeachLineLeaf(arc.getSite(),
					arc.getFace());
			centerLeaf = new BeachLineLeaf(site, new Face(id, site, faces,
					limits));

			BeachLineLeaf predecessor = arc.getPredecessor();
			BeachLineLeaf successor = arc.getSuccessor();

			if (predecessor != null) {
				predecessor.setSuccessor(leftLeaf);
			}
			if (successor != null) {
				successor.setPredecessor(rightLeaf);
			}

			leftLeaf.setPredecessor(predecessor);
			leftLeaf.setSuccessor(centerLeaf);
			rightLeaf.setPredecessor(centerLeaf);
			rightLeaf.setSuccessor(successor);
			centerLeaf.setPredecessor(leftLeaf);
			centerLeaf.setSuccessor(rightLeaf);

			BeachLineInternal lowerNode = new BeachLineInternal(centerLeaf,
					rightLeaf, site, arc.getSite());
			upperNode = new BeachLineInternal(leftLeaf, lowerNode,
					arc.getSite(), site);

			upperNode.setParent(parent);
			lowerNode.setParent(upperNode);
			centerLeaf.setParent(lowerNode);
			rightLeaf.setParent(lowerNode);
			leftLeaf.setParent(upperNode);
		} else {
			parent = arc.getParent();

			BeachLineLeaf leftLeaf = new BeachLineLeaf(arc.getSite(),
					arc.getFace());
			centerLeaf = new BeachLineLeaf(site, new Face(id, site, faces,
					limits));

			BeachLineLeaf predecessor = arc.getPredecessor();
			BeachLineLeaf successor = arc.getSuccessor();

			if (predecessor != null) {
				predecessor.setSuccessor(leftLeaf);
			}
			if (successor != null) {
				successor.setPredecessor(centerLeaf);
			}

			leftLeaf.setPredecessor(predecessor);
			leftLeaf.setSuccessor(centerLeaf);
			centerLeaf.setPredecessor(leftLeaf);
			centerLeaf.setSuccessor(successor);

			upperNode = new BeachLineInternal(leftLeaf, centerLeaf,
					arc.getSite(), site);

			upperNode.setParent(parent);
			centerLeaf.setParent(upperNode);
			leftLeaf.setParent(upperNode);
		}

		if (parent != null) {
			parent.replaceNode(arc, upperNode);
		} else {
			root = upperNode;
		}

		return centerLeaf;
	}

	BeachLineInternal deleteLeaf(BeachLineLeaf leaf) {
		BeachLineInternal breakPoint;

		BeachLineInternal parent = leaf.getParent();
		BeachLineNode siblingNode;
		VPoint siblingSite;

		leaf.getPredecessor().setSuccessor(leaf.getSuccessor());
		leaf.getSuccessor().setPredecessor(leaf.getPredecessor());

		if (parent.getLeftChild() == leaf) {
			siblingNode = parent.getRightChild();
			siblingSite = parent.getRightSite();
			replace(parent, siblingNode);

			breakPoint = getLeftBreakPoint(siblingNode);
			if (breakPoint != null) {
				breakPoint.setRightSite(siblingSite);
			}
		} else if (parent.getRightChild() == leaf) {
			siblingNode = parent.getLeftChild();
			siblingSite = parent.getLeftSite();
			replace(parent, siblingNode);

			breakPoint = getRightBreakPoint(siblingNode);
			if (breakPoint != null) {
				breakPoint.setLeftSite(siblingSite);
			}
		} else {
			throw new IllegalArgumentException();
		}

		return breakPoint;
	}

	BeachLineInternal getLeftBreakPoint(BeachLineNode node) {
		BeachLineInternal parent = node.getParent();

		if (parent == null) {
			return null;
		} else if (parent.getLeftChild() != node) {
			return parent;
		} else {
			return getLeftBreakPoint(parent);
		}
	}

	BeachLineInternal getRightBreakPoint(BeachLineNode node) {
		BeachLineInternal parent = node.getParent();

		if (parent == null) {
			return null;
		} else if (parent.getRightChild() != node) {
			return parent;
		} else {
			return getRightBreakPoint(parent);
		}
	}

	private void replace(BeachLineNode toReplace, BeachLineNode replacement) {
		BeachLineInternal parent = toReplace.getParent();

		replacement.setParent(parent);

		if (root == toReplace) {
			root = replacement;
		} else {
			if (parent.getLeftChild() == toReplace) {
				parent.setLeftChild(replacement);
			} else if (parent.getRightChild() == toReplace) {
				parent.setRightChild(replacement);
			} else {
				throw new IllegalStateException();
			}
		}
	}

	BeachLineInternal getLeftInternal() {
		BeachLineNode node = root;

		while (node.getClass() == BeachLineInternal.class) {
			node = node.getParent();
		}

		return node.getParent();
	}

	// DEBUG
	void printTree() {

		List<BeachLineNode> nodes = new LinkedList<BeachLineNode>();
		List<BeachLineNode> nextNodes = new LinkedList<BeachLineNode>();
		nodes.add(root);
		BeachLineNode left, right;

		System.out.println();

		while (!nodes.isEmpty()) {
			for (BeachLineNode n : nodes) {
				System.out.print(n.hashCode() + " ");

				if (n.getClass() == BeachLineInternal.class) {
					BeachLineInternal internal = (BeachLineInternal) n;
					left = internal.getLeftChild();
					right = internal.getRightChild();
					nextNodes.add(left);
					nextNodes.add(right);
				}
			}
			System.out.println();

			nodes = nextNodes;
			nextNodes = new LinkedList<BeachLineNode>();
		}

	}
}
