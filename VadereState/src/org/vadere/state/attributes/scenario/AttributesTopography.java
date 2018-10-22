package org.vadere.state.attributes.scenario;

import java.awt.geom.Rectangle2D;

import org.vadere.state.attributes.Attributes;
import org.vadere.geometry.shapes.VRectangle;

public class AttributesTopography extends Attributes {

	// private double finishTime = 500; // moved to AttributesSimulation
	private VRectangle bounds = new VRectangle(0, 0, 10, 10);
	private double boundingBoxWidth = 0.5;
	private boolean bounded = true;

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

	public void setBounds(VRectangle bounds) {
		checkSealed();
		this.bounds = bounds;
	}

}
