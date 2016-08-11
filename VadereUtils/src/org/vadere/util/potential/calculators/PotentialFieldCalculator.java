package org.vadere.util.potential.calculators;

import java.awt.Point;

/**
 * A PotentialFieldInitializer initializes and updates a given potential field.
 * The potential field may be applied in the constructor of the implementing
 * class and currently consists of a Grid which holds the environment data for
 * creation of the potential field as well as the potential values itself. The
 * initializer may either create a static floor field, that does not change over
 * time, or a dynamic floor field which may be updated during simulation.
 */
public interface PotentialFieldCalculator {

	/**
	 * Computes potentials on basis of the given data. Should be called from
	 * outside only once for initialization.
	 */
	void initialize();

	/**
	 * Recomputes the potentials. May be called every simulation step. May
	 * contain an empty implementation for static floor field initializers.
	 */
	void update();

	/**
	 * Returns true if the potential field needs an update. The value indicates
	 * the type of initializer: static or dynamic floor field. Initializer of
	 * static floor fields may return false, initializers of dynamic floor
	 * fields may return true.
	 */
	boolean needsUpdate();

	double getValue(final Point point);
}
