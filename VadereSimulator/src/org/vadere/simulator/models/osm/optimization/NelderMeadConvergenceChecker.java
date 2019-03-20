package org.vadere.simulator.models.osm.optimization;

import org.apache.commons.math.optimization.RealConvergenceChecker;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.util.MathUtils;

/**
 * The Class NelderMeadConvergenceChecker. The implementation of the stopping
 * criteria from [Nelder u. Mead, 1965].
 * 
 */

public class NelderMeadConvergenceChecker implements RealConvergenceChecker {

	private int lastiteration;

	private double threshold;

	private RealPointValuePair[] prev;

	private RealPointValuePair[] curr;

	private int index;

	private int dimension;

	private final int maxIterations;

	/**
	 * Instantiates a new convergence checker for NelderMead.
	 */
	NelderMeadConvergenceChecker() {
		lastiteration = -1;
		threshold = 0.01; // 100. * MathUtils.EPSILON;
		dimension = 3;
		prev = new RealPointValuePair[dimension];
		curr = new RealPointValuePair[dimension];
		index = 0;
		maxIterations = 100;
	}

	@Override
	/** Convergence criterion according to sivers-2013 (p.34) / nelder-1965 p.309
	 *
	 */
	public boolean converged(final int iteration,
			final RealPointValuePair previous, final RealPointValuePair current) {

		if (lastiteration != iteration) {
			index = 0;
			lastiteration = iteration;
		}

		prev[index] = previous;
		curr[index] = current;
		index++;

		if (index == dimension) {
			double meanValue = 0;
			double value = 0;
			for (int i = 0; i < dimension; i++) {
				meanValue += curr[i].getValue();
			}
			meanValue /= dimension;
			for (int i = 0; i < dimension; i++) {
				value += Math.pow(curr[i].getValue() - meanValue, 2);
			}
			return (value / dimension < threshold || iteration > maxIterations); // earlier 1000 => change to parameter
		} else {
			return true;
		}

	}

}
