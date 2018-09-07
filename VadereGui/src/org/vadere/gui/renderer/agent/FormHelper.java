package org.vadere.gui.renderer.agent;

import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.awt.geom.Path2D;


public class FormHelper {

	public static VPolygon getShape(int id, VPoint p, double r) {

		int form = id % 6;

		if (form == 0) {
			return getPentagon(p, r);
		} else if (form == 1) {
			return getDiamond(p, r);
		} else if (form == 2) {
			return getTriangle(p, r);
		} else if (form == 4) {
			return getStar5(p, r);
		} else if (form == 5) {
			return getStarX(p, r);
		} else {
			return getStar7(p, r);
		}
	}

	private static VPolygon getPentagon(VPoint center, double r) {

		return polygon(center, r, 5);
	}

	private static VPolygon getDiamond(VPoint center, double r) {
		return polygon(center, r, 4);
	}

	private static VPolygon getTriangle(VPoint center, double r) {
		return polygon(center, r, 3);
	}

	private static VPolygon getStar7(VPoint center, double r) {
		return star(center, r, 7);
	}

	private static VPolygon polygon(VPoint center, double r, int corners) {
		VPoint p = new VPoint(0, r);
		Path2D.Double path = new Path2D.Double();
		path.moveTo(center.x + p.x, center.y + p.y);

		for (int i = 0; i < corners; i++) {
			p = p.rotate(2 * Math.PI / corners);
			path.lineTo(center.x + p.x, center.y + p.y);
		}
		return new VPolygon(path);
	}

	private static VPolygon getStar5(VPoint center, double r) {
		return star(center, r, 5);
	}

	private static VPolygon getStarX(VPoint center, double r) {

		return star(center, r, 4).rotate(center, Math.PI / 4);
	}

	private static VPolygon star(VPoint center, double r, int corners) {
		VPoint p = new VPoint(0, r);
		Path2D.Double path = new Path2D.Double();
		path.moveTo(center.x + p.x, center.y + p.y);

		double alpha = Math.PI / corners;
		for (int i = 0; i < corners; i++) {
			p = p.rotate(alpha);
			path.lineTo(center.x + p.x, center.y + p.y);
			p = p.scalarMultiply(0.5);
			path.lineTo(center.x + p.x, center.y + p.y);
			p = p.rotate(alpha);
			path.lineTo(center.x + p.x, center.y + p.y);
			p = p.scalarMultiply(2.0);
			path.lineTo(center.x + p.x, center.y + p.y);
		}

		VPolygon polygon = new VPolygon(path);

		return polygon.rotate(center, -0.5 * alpha);
	}
}
