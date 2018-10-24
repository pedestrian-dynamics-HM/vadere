package org.vadere.meshing;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Benedikt Zoennchen
 */
public class ComparablePoint extends VPoint implements
		Comparable<ComparablePoint> {
	private static final long serialVersionUID = -674962334295807561L;

	public ComparablePoint(double x, double y) {
		super(x, y);
	}

	public ComparablePoint(VPoint position) {
		this(position.x, position.y);
	}

	/**
	 * Checks whether the given point is greater than the current point with
	 * respect to:
	 * <ol>
	 *     <li>
	 *         x-coordinate and then to the
	 *     </li>
	 *     <li>
	 *         y-coordinate
	 *     </li>
	 * </ol>
	 * 
	 * @param p
	 *        point to compare with
	 * @return 1 if the current point is greater than p, -1 if smaller, 0
	 *         otherwise.
	 */
	@Override
	public int compareTo(ComparablePoint p) {
		if (Math.abs(this.x - p.x) < GeometryUtils.DOUBLE_EPS) {
			if (Math.abs(this.y - p.y) < GeometryUtils.DOUBLE_EPS) {
				return 0;
			} else {
				if (this.y > p.y) {
					return 1;
				}
			}
		} else {
			if (this.x > p.x) {
				return 1;
			} else {
				return -1;
			}
		}
		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		ComparablePoint objP = (ComparablePoint) obj;

		if (this.compareTo(objP) == 0)
			return true;
		return false;
	};
}
