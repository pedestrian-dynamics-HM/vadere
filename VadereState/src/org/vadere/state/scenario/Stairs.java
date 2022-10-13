package org.vadere.state.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.AttributesStairs;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

public class Stairs extends ScenarioElement<AttributesStairs> {

	private static Logger logger = Logger.getLogger(Stairs.class);
	public static double MIN_TREAD_DEPTH = 0.100;
	public static double MAX_TREAD_DEPTH = 0.350;

	public static class Tread {
		public final VLine treadline;

		public Tread(VLine treadline) {
			this.treadline = treadline;
		}
	}

	private Tread[] treads;
	private double treadDepth;

	public Stairs(AttributesStairs attributes) {
		setAttributes(attributes);
	}

	private Tread[] computeTreads() {
		// tread count + 2 since the first and last treads must be placed outside of the shape and
		// on the next floor.
		Tread[] treadsResult = new Tread[this.getAttributes().getTreadCount() + 2];

		double angle = this.getAttributes().getUpwardDirection().angleToZero();

		PathIterator iterator = this.getShape().getPathIterator(AffineTransform.getRotateInstance(-angle));
		Path2D.Double p = new Path2D.Double();
		p.append(iterator, false);

		Rectangle2D rotatedBounds = new VPolygon(p).getBounds2D();

		treadDepth = rotatedBounds.getWidth() / this.getAttributes().getTreadCount();

		for (int i = 0; i < treadsResult.length; i++) {
			double factor = ((double) i) / treadsResult.length;

			// subtract one on the left and add one tread depth on the right so that the last and
			// next floors gets one tread too

			// by dividing treadDepth by 2 the lines are centered between the edges

			// __________________________________ << upper edge of tread
			// ---------------------------------- << line that pedestrians walk on (centered between edges)
			// __________________________________ << lower edge of (same) tread

			// This is the assumption that pedestrians always step in the middle of a tread.

			double x = rotatedBounds.getMinX() - treadDepth / 2 + factor * (rotatedBounds.getWidth() + treadDepth * 2);
			VPoint p1 = new VPoint(x, rotatedBounds.getMinY()).rotate(angle);
			VPoint p2 = new VPoint(x, rotatedBounds.getMaxY()).rotate(angle);

			VLine line = new VLine(p1, p2);

			treadsResult[i] = new Tread(line);
		}

		return treadsResult;
	}

	@Override
	public void setAttributes(AttributesStairs attributes) {
		this.attributes = attributes;
		treads = computeTreads();
	}

	@Override
	public void setShape(VShape newShape) {
		attributes.setShape(newShape);
	}

	@Override
	public VShape getShape() {
		return attributes.getShape();
	}

	@Override
	public int getId() {
		return attributes.getId();
	}

	@Override
	public void setId(int id) {
		attributes.setId(id);
	}

	public Tread[] getTreads() {
		return this.treads;
	}

	public double getTreadDepth() {
		return treadDepth;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Stairs)) {
			return false;
		}
		Stairs other = (Stairs) obj;
		if (attributes == null) {
			if (other.attributes != null) {
				return false;
			}
		} else if (!attributes.equals(other.attributes)) {
			return false;
		}
		return true;
	}

	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.STAIRS;
	}

	@Override
	public AttributesStairs getAttributes() {
		return attributes;
	}

	@Override
	public Stairs clone() {
		return new Stairs((AttributesStairs) attributes.clone());
	}

}
