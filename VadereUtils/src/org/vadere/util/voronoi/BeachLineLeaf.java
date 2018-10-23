package org.vadere.util.voronoi;

import org.vadere.util.geometry.shapes.VPoint;

public class BeachLineLeaf implements BeachLineNode {

	public static final double NUM_ERROR = 0.0001;

	private final VPoint site;
	private BeachLineInternal parent;
	private EventCircle circleEvent;

	private BeachLineLeaf predecessor;
	private BeachLineLeaf successor;

	private final Face face;

	BeachLineLeaf(VPoint site, Face face) {
		this.site = site;
		this.face = face;
	}

	public Face getFace() {
		return face;
	}

	EventCircle getCircleEvent() {
		return circleEvent;
	}

	void setCircleEvent(EventCircle circleEvent) {
		this.circleEvent = circleEvent;
	}

	@Override
	public BeachLineInternal getParent() {
		return parent;
	}

	@Override
	public void setParent(BeachLineInternal parent) {
		this.parent = parent;
	}

	VPoint getSite() {
		return site;
	}

	void setPredecessor(BeachLineLeaf predecessor) {
		this.predecessor = predecessor;
	}

	BeachLineLeaf getPredecessor() {
		return predecessor;
	}

	void setSuccessor(BeachLineLeaf successor) {
		this.successor = successor;
	}

	BeachLineLeaf getSuccessor() {
		return successor;
	}

	static int upCounter = 0;
	static int downCounter = 0;

	boolean hasCircleEvent() {
		boolean result = false;

		if (predecessor != null && successor != null) {
			double yPre = predecessor.getSite().y;
			double ySuc = successor.getSite().y;
			double yThis = site.y;
			double xPre = predecessor.getSite().x;
			double xSuc = successor.getSite().x;
			double xThis = site.x;

			double criteria = (xThis - xPre) * (yPre - ySuc) + (yThis - yPre)
					* (xSuc - xPre);

			if (criteria > NUM_ERROR) {
				result = true;
				upCounter++;
			} else {
				/*
				 * System.out.println(yPre + " " + yThis + " " + ySuc); if(
				 * (yThis + NUM_ERROR < yPre || yThis - NUM_ERROR > yPre ||
				 * yThis + NUM_ERROR < ySuc || yThis - NUM_ERROR > ySuc) ) {
				 * result = true; }
				 */
				downCounter++;
			}
		}

		return result;
	}

	// see: http://paulbourke.net/geometry/circlefrom3/
	void createCircleEvent() {
		double x, y;

		double yPre = predecessor.getSite().y;
		double xPre = predecessor.getSite().x;
		double ySuc = successor.getSite().y;
		double xSuc = successor.getSite().x;
		double yThis = site.y;
		double xThis = site.x;

		/*
		 * Formula looks like this:
		 * 
		 * double mA = (yThis - yPre) / (xThis - xPre); double mB = (ySuc -
		 * yThis) / (xSuc - xThis); double x = (mA*mB*(yPre - ySuc) + mB*(xPre +
		 * xThis) - mA*(xThis + xSuc)) / (2*(mB - mA));
		 * 
		 * The denominator can be 0. Manipulation with Mathematica yields the
		 * following, which seems better to me:
		 */
		x = (xThis * xThis * (yPre - ySuc)
				+ (xPre * xPre + (yPre - ySuc) * (yPre - yThis))
						* (ySuc - yThis)
				+ xSuc * xSuc * (-yPre + yThis));
		x = x
				/ (2 * (xThis * (yPre - ySuc) + xPre * (ySuc - yThis) + xSuc
						* (-yPre + yThis)));

		// conditions I thought of my own, hence it may be wrong
		if (yThis + NUM_ERROR < yPre || yThis - NUM_ERROR > yPre) {
			y = -(xThis - xPre) / (yThis - yPre) * (x - (xPre + xThis) / 2)
					+ (yPre + yThis) / 2;
		}
		/* else if(yThis + NUM_ERROR < ySuc || yThis - NUM_ERROR > ySuc) { */
		else {
			y = -(xSuc - xThis) / (ySuc - yThis) * (x - (xThis + xSuc) / 2)
					+ (yThis + ySuc) / 2;
		}
		/*
		 * else { throw new IllegalStateException(); }
		 */

		double radius = Math.sqrt(Math.pow(xThis - x, 2)
				+ Math.pow(yThis - y, 2));

		circleEvent = new EventCircle(x, y, this, y - radius, x);
	}

	public EventCircle removeCircleEvent() {
		EventCircle result = circleEvent;
		this.circleEvent = null;
		return result;
	}
}
