package org.vadere.util.geometry;

import org.vadere.geometry.Utils;
import org.vadere.geometry.Vector2D;
import org.vadere.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeometryUtils {

	/**
	 * Orders a given list angular relative to a given point, starting with
	 * angle 0.
	 *
	 * @param allPoints
	 * @param center
	 * @return an ordered DataPoint list with the angle of the point as data and
	 *         the original index set.
	 */
	public static List<DataPoint> orderByAngle(List<VPoint> allPoints,
	                                           VPoint center) {
		List<DataPoint> orderedList = new ArrayList<DataPoint>();

		for (int i = 0; i < allPoints.size(); i++) {
			Vector2D p = new Vector2D(allPoints.get(i));
			orderedList.add(new DataPoint(p.x, p.y, Utils.angleTo(p, center)));
		}
		// sort by angle
		Collections.sort(orderedList, DataPoint.getComparator());

		return orderedList;
	}

}
