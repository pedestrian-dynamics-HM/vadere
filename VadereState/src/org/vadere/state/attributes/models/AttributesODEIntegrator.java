package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.IntegratorType;

/**
 * Provides attributes for ODE integrators:<br>
 * IntegratorType, minStepSize, maxStepSize, absoluteTolerance,
 * relativeTolerance. For methods without step size control, minStepSize and
 * maxStepSize should be equal.
 * 
 * 
 */
@ModelAttributeClass
public class AttributesODEIntegrator extends Attributes {
	private IntegratorType solverType = IntegratorType.DORMAND_PRINCE_45;
	private double stepSizeMin = 1e-4;
	private double stepSizeMax = 1.0;
	private double toleranceAbsolute = 1e-5;
	private double toleranceRelative = 1e-4;

	// Getters...
	public IntegratorType getSolverType() {
		return solverType;
	}

	public double getStepSizeMin() {
		return stepSizeMin;
	}

	public double getStepSizeMax() {
		return stepSizeMax;
	}

	public double getToleranceAbsolute() {
		return toleranceAbsolute;
	}

	public double getToleranceRelative() {
		return toleranceRelative;
	}
}
