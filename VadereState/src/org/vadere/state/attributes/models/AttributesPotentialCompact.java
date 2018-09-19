package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesPotentialCompact extends Attributes {

	// queueing
	private double pedPotentialWidth = 0.5;

	// queueing
	private double pedPotentialHeight = 12.6;

	// queueing
	private double obstPotentialWidth = 0.25;

	// queueing
	private double obstPotentialHeight = 20.1;

	// queueing
	private boolean useHardBodyShell = false;

	// queueing
	private double obstDistanceDeviation = 0;

	// queueing
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
