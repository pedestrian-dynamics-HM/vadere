package org.vadere.util.voronoi;

import java.util.List;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.MathUtil;

/**
 * Internal node of the Red-Black Tree data structure of the Beach Line.
 */
public class BeachLineInternal implements BeachLineNode {
	private BeachLineNode leftChild;
	private BeachLineNode rightChild;
	private BeachLineInternal parent;

	private VPoint leftSite;
	private VPoint rightSite;

	private HalfEdge halfEdge;

	BeachLineInternal(BeachLineNode leftChild, BeachLineNode rightChild,
			VPoint leftSite, VPoint rightSite) {
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.leftSite = leftSite;
		this.rightSite = rightSite;
	}

	double getBreakPoint(double sweepLine) {

		// desired intersection of the two parabolas
		double result;

		// TODO [priority=medium] [task=refactoring] QuickFix for avoid exception if both points are equals
		if (leftSite.equals(rightSite)) {
			rightSite = new VPoint(rightSite.x + 0.000000001, rightSite.y);
		}

		double xA = leftSite.x;
		double yA = leftSite.y;
		double xB = rightSite.x;
		double yB = rightSite.y;

		if (Math.abs(yA - sweepLine) < 0.000001) {
			return xA;
		}

		if (Math.abs(yB - sweepLine) < 0.000001) {
			return xB;
		}

		double coefA = 1 / (2 * (yA - sweepLine));
		double coefB = 1 / (2 * (yB - sweepLine));

		// coefficients of quadratic equation
		double a = coefA - coefB;
		double b = coefA * (-2 * xA) - coefB * (-2 * xB);
		double c = coefA
				* (Math.pow(xA, 2) + Math.pow(yA, 2) - Math.pow(sweepLine, 2));
		c = c - coefB
				* (Math.pow(xB, 2) + Math.pow(yB, 2) - Math.pow(sweepLine, 2));

		// all intersections of the parabolas
		List<Double> sol = MathUtil.solveQuadratic(a, b, c);

		if (sol.size() == 2) {
			if (leftSite.y > rightSite.y) {
				result = Math.min(sol.get(0), sol.get(1));
			} else {
				result = Math.max(sol.get(0), sol.get(1));
			}
		} else if (sol.size() == 1) {
			result = sol.get(0);
		} else {
			throw new IllegalStateException(
					"There must be at least one intersection of the parabolas: leftPoint="
							+ leftSite + ", rightPoint=" + rightSite);
		}

		return result;
	}

	BeachLineNode getRightChild() {
		return rightChild;
	}

	void setRightChild(BeachLineNode rightChild) {
		this.rightChild = rightChild;
	}

	BeachLineNode getLeftChild() {
		return leftChild;
	}

	void setLeftChild(BeachLineNode leftChild) {
		this.leftChild = leftChild;
	}

	VPoint getRightSite() {
		return rightSite;
	}

	VPoint getLeftSite() {
		return leftSite;
	}

	HalfEdge getHalfEgde() {
		return halfEdge;
	}

	void setHalfEdge(HalfEdge halfEdge) {
		this.halfEdge = halfEdge;
	}

	void replaceNode(BeachLineLeaf arc, BeachLineInternal upperNode) {
		if (leftChild == arc) {
			leftChild = upperNode;
		} else if (rightChild == arc) {
			rightChild = upperNode;
		} else {
			throw new IllegalArgumentException(
					"The arc is not a child of this node.");
		}
	}

	@Override
	public BeachLineInternal getParent() {
		return parent;
	}

	@Override
	public void setParent(BeachLineInternal parent) {
		this.parent = parent;
	}

	public void setLeftSite(VPoint leftSite) {
		this.leftSite = leftSite;
	}

	public void setRightSite(VPoint rightSite) {
		this.rightSite = rightSite;
	}
}
