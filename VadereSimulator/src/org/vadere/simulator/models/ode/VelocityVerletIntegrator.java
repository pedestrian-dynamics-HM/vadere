package org.vadere.simulator.models.ode;

import java.util.Arrays;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.exception.util.Localizable;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.ode.AbstractIntegrator;
import org.apache.commons.math3.ode.ExpandableStatefulODE;

/**
 * Basic velocity verlet method from
 * http://en.wikipedia.org/wiki/Verlet_integration
 * 
 * Uses the following format to extract positions and velocities:
 * inputarray: y
 * format: y = [x1, y1, vx1, xy1, x2, y2, vx2, vy2, ...]
 * where the numbers indicate particles, x,y indicate position and vx,vy indicate velocities.
 * 
 *
 */
public class VelocityVerletIntegrator extends AbstractIntegrator {

	public VelocityVerletIntegrator(double stepSizeMin) {
		this.stepSize = stepSizeMin;
	}

	@Override
	public void integrate(ExpandableStatefulODE equations, double t)
			throws MathIllegalStateException, MathIllegalArgumentException {
		int n = equations.getTotalDimension();
		if (n % 2 != 0 || n < 2)
			throw new MathIllegalArgumentException(LocalizedFormats.ARGUMENT_OUTSIDE_DOMAIN, this.getClass().getName());

		double[] y = Arrays.copyOf(equations.getCompleteState(), n);
		double[] yDot = new double[n];
		double[] x = new double[n / 2];
		double[] v = new double[n / 2];
		double[] a = new double[n / 2];
		double Dt = t - equations.getTime();
		double dt = this.stepSize;

		// compute the forces for the first time
		equations.computeDerivatives(t, y, yDot);

		// initialize velocity, position and forces
		for (int i = 0; i < n / 4; i++) {
			x[i * 2] = y[i * 4];
			x[i * 2 + 1] = y[i * 4 + 1];
			v[i * 2] = y[i * 4 + 2];
			v[i * 2 + 1] = y[i * 4 + 3];
			a[i * 2] = yDot[i * 4 + 2];
			a[i * 2 + 1] = yDot[i * 4 + 3];
		}

		int steps = (int) Math.ceil(Dt / dt);

		for (int step = 0; step < steps; step++) {
			// calculate v(t+0.5 dt)=v(t)+0.5*a(t) dt
			for (int i = 0; i < v.length; i++) {
				v[i] = v[i] + 0.5 * a[i] * dt;
			}

			// calculate x(t+dt)=x(t) + v(t+0.5*dt)*dt
			for (int i = 0; i < n / 4; i++) {
				x[i * 2] = x[i * 2] + v[i * 2] * dt;
				x[i * 2 + 1] = x[i * 2 + 1] + v[i * 2 + 1] * dt;

				y[i * 4] = x[i * 2];
				y[i * 4 + 1] = x[i * 2 + 1];
			}

			// derive a(t+dt) from x'(t+dt), i.e. a(t+dt) = -(1/m)F(x(t+dt))
			equations.computeDerivatives(t, y, yDot);
			// initialize velocity, position and forces
			for (int i = 0; i < n / 4; i++) {
				a[i * 2] = yDot[i * 4 + 2];
				a[i * 2 + 1] = yDot[i * 4 + 3];
			}

			// calculate v(t+dt)=v(t+0.5*dt)+0.5*a(t+dt)*dt
			for (int i = 0; i < v.length; i++) {
				v[i] = v[i] + 0.5 * a[i] * dt;
			}
		}

		// reshift x and v into y
		for (int i = 0; i < n / 4; i++) {
			y[i * 4] = x[i * 2];
			y[i * 4 + 1] = x[i * 2 + 1];
			y[i * 4 + 2] = v[i * 2];
			y[i * 4 + 3] = v[i * 2 + 1];
		}
		equations.setCompleteState(y);
	}


}
