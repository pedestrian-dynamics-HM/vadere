package org.vadere.util.geometry;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.Comparator;

public class CCWComparator implements Comparator<IPoint> {

	private final IPoint center;

	public CCWComparator(final IPoint center) {
		this.center = center;
	}

	@Override
	public int compare(IPoint a, IPoint b) {
		if(a.getX() - center.getX() >= 0 && b.getX() - center.getX() < 0) {
			return -1;
		}

		if(a.getX() - center.getX() < 0 && b.getX() - center.getX() >= 0) {
			return 1;
		}

		if(a.getX() - center.getX() == 0 && b.getX() - center.getX() == 0) {
			if(a.getY() - center.getY() >= 0 || b.getY() - center.getY() >= 0) {
				return a.getY() > b.getY() ? -1 : 1;
			}
			return b.getY() > a.getY() ? -1 : 1;
		}

		// cross product
		double det = (a.getX() - center.getX()) * (b.getY() - center.getY()) - (b.getX() - center.getX()) * (a.getY() - center.getY());

		if(det < 0) {
			return 1;
		}
		else if(det > 0) {
			return -1;
		}
		else {
			return a.distanceSq(center) > b.distanceSq(center) ? -1 : 1;
		}
	}

}
