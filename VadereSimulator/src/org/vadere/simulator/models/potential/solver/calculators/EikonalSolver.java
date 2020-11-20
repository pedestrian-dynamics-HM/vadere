package org.vadere.simulator.models.potential.solver.calculators;

import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.simulator.models.potential.solver.timecost.UnitTimeCostFunction;
import org.vadere.simulator.utils.cache.ICacheObject;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.logging.Logger;

import java.util.function.Function;

/**
 * The eikonal solver, solves the eikonal equation on a Cartesian grid. In case of a changing F the
 * solver re-computes the solution if update is called.
 */
public interface EikonalSolver {

	Logger logger = Logger.getLogger(EikonalSolver.class);

	enum Direction {
		UP_LEFT, UP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, ANY;
	}

	/**
	 * Computes potentials on basis of the given data. Should be called from
	 * outside only once for initialization.
	 */
	void solve();

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

	default double getPotential(final IPoint pos, final double unknownPenalty, final double weight, final Object caller) {
		return getPotential(pos, unknownPenalty, weight);
	}

	/**
	 * Returns a copy of the current (for the current F which might change over time) solution of the eikonal equation.
	 *
	 * @return a copy of the current solution of the eikonal equation
	 */
	Function<IPoint, Double> getPotentialField();

	default double getPotential(final IPoint pos) {
		return getPotential(pos.getX(), pos.getY());
	}

	default double getPotential(final IPoint pos, final Object caller) {
		return getPotential(pos.getX(), pos.getY(), caller);
	}

	double getPotential(final double x, final double y);

	default double getPotential(final double x, final double y, Object caller) {
		return getPotential(x, y);
	}

	default boolean isHighAccuracy() {
		return true;
	}

	default ITimeCostFunction getTimeCostFunction() {
		return new UnitTimeCostFunction();
	}

	/**
	 * Load floor field from given file path. This method does not check if the cached floor field
	 * is up to date with the current state of the scenario. This must be checked by the caller
	 * beforehand.
	 *
	 * The default behavior does not support caching. To support caching override this method and
	 * implement the needed logic.
	 * see {@link org.vadere.simulator.models.potential.solver.calculators.cartesian.GridEikonalSolver}
	 * for implementation.
	 *
	 * @param cacheObject path to cached floor field
	 * @return true if floor field could be loaded and false if not.
	 */
	default boolean loadCachedFloorField(ICacheObject cacheObject){
		// default not implemented. This will force a rebuild for each EikonalSolver at creation time.
		logger.infof("caching not implemented for given EikonalSolver %s", this.getClass().getName());
		return false;
	}

	/**
	 * Save floor field to given file path.
	 *
	 * The default behavior does not support caching. To support caching override this method and
	 * implement the needed logic.
	 * see {@link org.vadere.simulator.models.potential.solver.calculators.cartesian.GridEikonalSolver}
	 * for implementation.
	 *  @param cache path to cached floor field
	 */
	default void saveFloorFieldToCache(ICacheObject cache){
		// default not implemented. This will force a rebuild for each EikonalSolver at creation time.
		logger.infof("caching not implemented for given EikonalSolver %s", this.getClass().getName());
	}

	IMesh<?, ?, ?> getDiscretization();
}
