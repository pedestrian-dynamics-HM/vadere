package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.ReferenceCoordinateSystem;
import org.vadere.util.geometry.shapes.VRectangle;

import java.awt.geom.Rectangle2D;

public class AttributesTopography extends Attributes {

	// private double finishTime = 500; // moved to AttributesSimulation
	private VRectangle bounds = new VRectangle(0, 0, 10, 10);
	private double boundingBoxWidth = 0.5;
	private boolean bounded = true;
	/**
	 *  Reference coordinate system to translate vadere topography to world cooridnate systems
	 *  such as UTM zones, or GPS lon lat.
	 */
	private ReferenceCoordinateSystem referenceCoordinateSystem = null;

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

	public ReferenceCoordinateSystem getReferenceCoordinateSystem() {
		return referenceCoordinateSystem;
	}

	public void setReferenceCoordinateSystem(ReferenceCoordinateSystem referenceCoordinateSystem) {
		checkSealed();
		this.referenceCoordinateSystem = referenceCoordinateSystem;
	}
}
