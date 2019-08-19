package org.vadere.util.math.optimization.neldermead;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the 1-dimensional Nelder-Mead. The algorithm walks on the circle.
 * Multiple simplexes can be solved in parallel (default).
 *
 * @author Benedikt Zoennchen
 */
public class NelderMead1D {

	private static final Logger logger = Logger.getLogger(NelderMead1D.class);
	static {
		logger.setInfo();
	}

	/**
	 * the step circle
	 */
	private final VCircle evalArea;

	/**
	 * a simplex instance converges if the area of the triangle is smaller than <tt>threshold</tt> and
	 */
	private final double threshold;

	/**
	 * list of simplexes i.e. each simplex represents one Nelder-Mead optimization
	 */
	private final List<Simplex1D> simplices;

	/**
	 * if true, the optimization runs in parallel.
	 */
	private boolean runParallel = true;

	/**
	 * if true, the optimization is a minimization, otherwise it is a maximization.
	 */
	private boolean minimize = true;

	/**
	 * the maximum number of iterations of a simplex.
	 */
	private static final int NMAX = 100;

	/**
	 * counts the required number of iterations.
	 */
	private int iterationCount;

	/**
	 * Construct a Nelder-Mead instance for minimization.
	 *
	 * @param evalArea      the evaluation area i.e. outside this area eval.apply() is equal to MAX_VAL
	 * @param eval          the evaluation function such that for illegal / invalid position p eval.apply(p) == MAX_VAL
	 * @param threshold     the threshold
	 * @param simplexes     the line-segments (on the circle defined in rad) defining the 1D simplexes
	 */
	public NelderMead1D(@NotNull final VCircle evalArea,
	                    @NotNull final Function<IPoint, Double> eval,
	                    final double threshold,
	                    @NotNull final Collection<Pair<Double, Double>> simplexes){

		this.evalArea = evalArea;
		this.threshold = threshold;
		this.simplices = simplexes.stream().map(s -> new Simplex1D(eval, s.getLeft(), s.getRight(), evalArea, minimize, NelderMead2D.MAX_VAL)).collect(Collectors.toList());
		this.iterationCount = 0;
	}

	/**
	 * search for the optimum.
	 */
	public void optimize() {
		// first run
		int overallIterations = 0;
		while (!hasConverged() && iterationCount < NMAX) {
			update();
			iterationCount++;
			overallIterations++;
		}
		if(iterationCount == NMAX) {
			logger.warn("max iteration reached!");
		}

		// second run
		/*simplices.stream().forEach(s -> s.restart(evalArea.getRadius() / 10.0));

		iterationCount = 0;
		while (!hasConverged() && iterationCount < NMAX) {
			update();
			iterationCount++;
			overallIterations++;
		}
		if(iterationCount == NMAX) {
			logger.warn("max iteration reached!");
		}

		logger.debug("iterations: " + overallIterations);*/

	}

	/**
	 * Returns the argument of the solution.
	 *
	 * @return the argument of the solution
	 */
	public VPoint getArg() {
		return getBest().getArg();
	}

	/**
	 * Returns the solution.
	 *
	 * @return the solution
	 */
	public double getValue() {
		return getBest().getValue();
	}

	/**
	 * Runs the next iteration for all simplexes which are not jet converged and not jet invalid.
	 */
	public void update() {
		stream().filter(s -> !s.isInvalid()).filter(s -> !s.hasConverged(threshold)).forEach(s -> s.update());
	}

	/**
	 * Returns the simplex which found the best solution.
	 *
	 * @return the simplex which of the best solution
	 */
	private Simplex1D getBest() {
		Simplex1D best = null;
		for (Simplex1D simplex : simplices) {
			if(best == null || (minimize && best.getValue() > simplex.getValue()) || (!minimize && best.getValue() < simplex.getValue())) {
				best = simplex;
			}
		}

		return best;
	}

	/**
	 * Returns true, if all valid simplexes have converged which depends on the <tt>threshold</tt>, false otherwise
	 *
	 * @return true if and only if all simplexes have converged.
	 */
	private boolean hasConverged() {
		return stream().filter(s -> !s.isInvalid()).allMatch(s -> s.hasConverged(threshold));
	}

	/**
	 * Returns a (parallel) stream of all simplexes.
	 *
	 * @return a (parallel) stream of all simplexes
	 */
	private Stream<Simplex1D> stream() {
		if(runParallel) {
			return simplices.parallelStream();
		}
		else {
			return simplices.stream();
		}
	}
}
