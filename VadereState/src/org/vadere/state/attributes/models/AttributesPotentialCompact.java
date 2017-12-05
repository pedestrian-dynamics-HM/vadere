package org.vadere.state.attributes.models;

import org.vadere.state.attributes.Attributes;

/**
 * reference: The effect of stepping on pedestrian trajectories (seitz-2015).
 *
 */
public class AttributesPotentialCompact extends Attributes {

	private double pedPotentialWidth = 0.5;
	private double pedPotentialHeight = 12.6;
	private double obstPotentialWidth = 0.25;
	private double obstPotentialHeight = 20.1;

	private boolean useHardBodyShell = false;
	private double obstDistanceDeviation = 0;

	private double visionFieldRadius = 5;


	public AttributesPotentialCompact() {}

	public double getPedPotentialWidth() {
		return pedPotentialWidth;
	}

	public double getPedPotentialHeight() {
		return pedPotentialHeight;
	}

	public double getObstPotentialWidth() {
		return obstPotentialWidth;
	}

	public double getObstPotentialHeight() {
		return obstPotentialHeight;
	}

	public double getVisionFieldRadius() {
		return visionFieldRadius;
	}

	public double getObstDistanceDeviation() {
		return obstDistanceDeviation;
	}

	public boolean isUseHardBodyShell() {
		return useHardBodyShell;
	}

}
