package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

@ModelAttributeClass
public class AttributesPotentialCompactSoftshell extends Attributes {

	private double pedPotentialIntimateSpaceWidth = 0.45;
	private double pedPotentialPersonalSpaceWidth = 1.20;
	private double pedPotentialHeight = 50.0;
	private double obstPotentialWidth = 0.8;
	private double obstPotentialHeight = 6.0;
	private double intimateSpaceFactor = 1.2;
	private int personalSpacePower = 1;
	private int intimateSpacePower = 1;

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
