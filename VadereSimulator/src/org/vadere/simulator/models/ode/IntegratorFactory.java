package org.vadere.simulator.models.ode;


import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince54Integrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.nonstiff.EulerIntegrator;
import org.vadere.state.attributes.models.AttributesODEIntegrator;

/**
 * Factory for several apache ODE system integrators.
 * 
 * 
 */
public class IntegratorFactory {

	public static FirstOrderIntegrator createFirstOrderIntegrator(
			AttributesODEIntegrator attributes) {
		FirstOrderIntegrator integrator;

		switch (attributes.getSolverType()) {
			case DORMAND_PRINCE_45:
				integrator = new DormandPrince54Integrator(
						attributes.getStepSizeMin(), attributes.getStepSizeMax(),
						attributes.getToleranceAbsolute(),
						attributes.getToleranceRelative());
				break;
			case DORMAND_PRINCE_83:
				integrator = new DormandPrince853Integrator(
						attributes.getStepSizeMin(), attributes.getStepSizeMax(),
						attributes.getToleranceAbsolute(),
						attributes.getToleranceRelative());
				break;
			case CLASSICAL_RK4:
				integrator = new ClassicalRungeKuttaIntegrator(
						attributes.getStepSizeMin());
				break;
			case EULER:
				integrator = new EulerIntegrator(attributes.getStepSizeMin());
				break;
			case VELOCITY_VERLET:
				integrator = new VelocityVerletIntegrator(attributes.getStepSizeMin());
				break;
			case NONE:
			default:
				throw new IllegalArgumentException(
						"No solver type specified. Aborting.");
		}

		return integrator;
	}

}
