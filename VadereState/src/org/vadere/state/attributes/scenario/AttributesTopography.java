package org.vadere.state.attributes.scenario;

import java.awt.geom.Rectangle2D;

import org.vadere.state.attributes.Attributes;
import org.vadere.util.geometry.shapes.VRectangle;

public class AttributesTopography extends Attributes {

	// private double finishTime = 500; // moved to AttributesSimulation
	private VRectangle bounds = new VRectangle(0, 0, 10, 10);
	private double boundingBoxWidth = 0.5;
	private boolean bounded = true;

	public AttributesTopography() {}

	// Getters...

	public double getBoundingBoxWidth() {
		return boundingBoxWidth;
	}

	/*
	 * public double getFinishTime() {
	 * return finishTime;
	 * }
	 */

	public Rectangle2D.Double getBounds() {
		return bounds;
	}

	public boolean isBounded() {
		return bounded;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (bounded ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(boundingBoxWidth);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((bounds == null) ? 0 : bounds.hashCode());
		// temp = Double.doubleToLongBits(finishTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		if (!(obj instanceof AttributesTopography)) {
			return false;
		}
		AttributesTopography other = (AttributesTopography) obj;
		if (bounded != other.bounded) {
			return false;
		}
		if (Double.doubleToLongBits(boundingBoxWidth) != Double
				.doubleToLongBits(other.boundingBoxWidth)) {
			return false;
		}
		if (bounds == null) {
			if (other.bounds != null) {
				return false;
			}
		} else if (!bounds.equals(other.bounds)) {
			return false;
		}
		/*
		 * if (Double.doubleToLongBits(finishTime) != Double
		 * .doubleToLongBits(other.finishTime))
		 * {
		 * return false;
		 * }
		 */
		return true;
	}



}
