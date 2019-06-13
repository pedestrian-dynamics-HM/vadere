package org.vadere.util.math.optimization.neldermead;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the 2-dimensional Nelder-Mead.
 * Multiple simplexes can be solved in parallel (default).
 *
 * @author Benedikt Zoennchen
 */
public class NelderMead2D {

	private static final Logger logger = Logger.getLogger(NelderMead2D.class);

	/**
	 * sets the logger level.
	 */
	static {
		logger.setInfo();
	}

	/**
	 * the step circle
	 */
	private final VCircle evalArea;

	/**
	 * the evaluation function which will be minimized or maximized.
	 */
	private final Function<IPoint, Double> eval;

	/**
	 * a simplex instance converges if the area of the triangle is smaller than <tt>threshold</tt> and
	 */
	private final double threshold;

	/**
	 * list of simplexes i.e. each simplex represents one Nelder-Mead optimization
	 */
	private final List<Simplex2D> simplexes;

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
	 * value at illegal positions if we minimize
	 */
	public static final double MAX_VAL = 10000;

	/**
	 * the value which is the solution (i.e. global maximum or minimum of the evaluation function).
	 */
	public static final double SOLUTION_VAL = 0;

	/**
	 * value at illegal positions if we maximize
	 */
	public static final double MIN_VAL = -MAX_VAL;

	/**
	 * counts the required number of iterations.
	 */
	private int iterationCount;

	/**
	 * Construct a Nelder-Mead instance for minimization.
	 *
	 * @param evalArea      the evaluation area i.e. outside this area eval.apply() is equal to MAX_VAL
	 * @param eval          the evaluation function such that for illegal / invalid position p eval.apply(p) == MAX_VAL
	 * @param threshold     the threashold
	 * @param simplexes     the trianlges defining the 2D simplexes
	 */
	public NelderMead2D(@NotNull final VCircle evalArea,
	                    @NotNull final Function<IPoint, Double> eval,
	                    final double threshold,
	                    @NotNull Collection<VTriangle> simplexes){

		this.evalArea = evalArea;
		this.eval = eval;
		this.threshold = threshold;
		this.simplexes = simplexes.stream().map(triangle -> new Simplex2D(eval, triangle)).filter(simplex2D -> !simplex2D.isInvalid()).collect(Collectors.toList());
		this.iterationCount = 0;
	}

	/**
	 * search for the optimum.
	 */
	public void optimize() {
		// first run
		int overallIterations = 0;
//		stream().forEach(s -> System.out.println(s));
		while (!hasConverged() && iterationCount < NMAX) {
			update();
			//stream().forEach(s -> System.out.println(s));
			iterationCount++;
			overallIterations++;
		}
		if(iterationCount == NMAX) {
			logger.warn("max iteration reached!");
		}

		// second run
		/*simplexes.stream().forEach(s -> s.restart(evalArea.getRadius() / 10.0));
		simplexes.removeIf(s -> !s.isInvalid());


		//stream().forEach(s -> System.out.println(s));
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
	private void update() {
		stream().filter(s -> !s.isInvalid()).filter(s -> !s.hasConverged(threshold)).forEach(s -> s.update());
	}

	/**
	 * Returns the simplex which found the best solution.
	 *
	 * @return the simplex which of the best solution
	 */
	private Simplex2D getBest() {
		Simplex2D best = null;
		for (Simplex2D simplex : simplexes) {
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
	private Stream<Simplex2D> stream() {
		if(runParallel) {
			return simplexes.parallelStream();
		}
		else {
			return simplexes.stream();
		}
	}

	@Override
	public String toString() {
		return "[" + simplexes.stream().map(simplex2D -> simplex2D.toString()).reduce((s1, s2) -> s1 + "," + s2).orElse("") + "]";
	}
}
