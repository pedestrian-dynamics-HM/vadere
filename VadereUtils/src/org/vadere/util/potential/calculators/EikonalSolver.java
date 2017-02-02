package org.vadere.util.potential.calculators;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.MathUtil;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.timecost.ITimeCostFunction;
import org.vadere.util.potential.timecost.UnitTimeCostFunction;

import java.awt.*;

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

	/**
	 *
	 * @param point
	 * @return
	 */
	/*default double getValue(final Point point, final CellGrid cellGrid) {
		return cellGrid.getValue(point).potential;
	}*/

	//CellGrid getPotentialField();

	double getValue(final double x, final double y);

	default double getValue(final IPoint position) {
		return getValue(position.getX(), position.getY());
	}

	default boolean isHighAccuracy() {
		return true;
	}

	default ITimeCostFunction getTimeCostFunction() {
		return new UnitTimeCostFunction();
	}

}
