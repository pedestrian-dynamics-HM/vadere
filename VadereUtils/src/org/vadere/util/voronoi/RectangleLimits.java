package org.vadere.util.voronoi;

import java.util.ArrayList;
import java.util.List;

import org.vadere.util.geometry.shapes.VPoint;

/* ToDo: Remove class. */
public class RectangleLimits {
	public final double xLow;
	public final double yLow;
	public final double xHigh;
	public final double yHigh;

	public final List<VPoint> corners;

	public RectangleLimits(double xLimitLow, double yLimitLow,
			double xLimitHigh, double yLimitHigh) {

		this.xLow = xLimitLow;
		this.yLow = yLimitLow;
		this.xHigh = xLimitHigh;
		this.yHigh = yLimitHigh;

		corners = new ArrayList<VPoint>(4);

		corners.add(new VPoint(xLimitLow, yLimitHigh));
		corners.add(new VPoint(xLimitLow, yLimitLow));
		corners.add(new VPoint(xLimitHigh, yLimitLow));
		corners.add(new VPoint(xLimitHigh, yLimitHigh));
	}

	public boolean isInside(VPoint site) {
		boolean result = true;

		VPoint position = site;

		if (position.x < xLow || position.x > xHigh || position.y < yLow
				|| position.y > yHigh) {
			result = false;
		}

		return result;
	}
}
