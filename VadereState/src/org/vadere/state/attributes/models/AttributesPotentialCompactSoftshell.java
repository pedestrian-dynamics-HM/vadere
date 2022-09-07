package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesPotentialCompactSoftshell extends Attributes {

	private final double pedPotentialIntimateSpaceWidth = 0.45;
	private final double pedPotentialPersonalSpaceWidth = 1.20;
	private final double pedPotentialHeight = 50.0;
	private final double obstPotentialWidth = 0.8;
	private final double obstPotentialHeight = 6.0;
	private final double intimateSpaceFactor = 1.2;
	private final int personalSpacePower = 1;
	private final int intimateSpacePower = 1;

	public int getIntimateSpacePower() {
		return intimateSpacePower;
	}

	public double getIntimateSpaceFactor() {
		return intimateSpaceFactor;
	}

	public double getPedPotentialIntimateSpaceWidth() {
		return pedPotentialIntimateSpaceWidth;
	}

	public double getPedPotentialPersonalSpaceWidth() {
		return pedPotentialPersonalSpaceWidth;
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

	public int getPersonalSpacePower() {
		return personalSpacePower;
	}

}
