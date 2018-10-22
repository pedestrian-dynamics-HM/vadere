package org.vadere.util.potential.calculators;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.geometry.shapes.IPoint;
import org.vadere.util.potential.timecost.ITimeCostFunction;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;

import java.util.function.Function;

/**
 * The eikonal solver, solves the eikonal equation on a Cartesian grid. In case of a changing F the
 * solver re-computes the solution if update is called.
 */
public interface EikonalSolver {

	Logger logger = LogManager.getLogger(EikonalSolver.class);

	enum Direction {
		UP_LEFT, UP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, ANY;
	}

	/**
	 * Computes potentials on basis of the given data. Should be called from
	 * outside only once for initialization.
	 */
	void initialize();

	/**
	 * Recomputes the potentials. May be called every simulation step. May
	 * contain an empty implementation for static floor field initializers.
	 */
	default void update() {}

	/**
	 * Returns true if the potential field needs an update. The value indicates
	 * the type of initializer: static or dynamic floor field. Initializer of
	 * static floor fields may return false, initializers of dynamic floor
	 * fields may return true.
	 */
	default boolean needsUpdate() {
		return false;
	}

	double getPotential(final IPoint pos, final double unknownPenalty, final double weight);

	/**
	 * Returns a copy of the current (for the current F which might change over time) solution of the eikonal equation.
	 *
	 * @return a copy of the current solution of the eikonal equation
	 */
	Function<IPoint, Double> getPotentialField();

	default double getPotential(final IPoint pos) {
		return getPotential(pos.getX(), pos.getY());
	}

	double getPotential(final double x, final double y);

	default boolean isHighAccuracy() {
		return true;
	}

	default ITimeCostFunction getTimeCostFunction() {
		return new UnitTimeCostFunction();
	}
}
