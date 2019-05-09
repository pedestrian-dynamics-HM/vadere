package org.vadere.util.math;

import org.junit.Test;

/**
 * @author Benedikt Zoennchen
 */
public class TestMachineEpsilon {

	@Test
	public void testMachineEpsilon() {
		assert Double.compare(Math.ulp(1.0), machineEpsilon(0.5)) == 0;
	}

	public static double machineEpsilon(double eps)
	{
		// taking a floating type variable
		double prev_epsilon = 0.0;

		// run until condition satisfy
		while ((1+eps) != 1)
		{
			// copying value of epsilon
			// into previous epsilon
			prev_epsilon = eps;

			// dividing epsilon by 2
			eps /=2;
		}

		return prev_epsilon;
	}
}
