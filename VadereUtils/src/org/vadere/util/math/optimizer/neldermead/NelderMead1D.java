package org.vadere.util.math.optimizer.neldermead;

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
 * @author Benedikt Zoennchen
 */
public class NelderMead1D {

	private static final Logger logger = Logger.getLogger(NelderMead1D.class);
	private final VCircle evalArea;
	private final Function<IPoint, Double> eval;
	private final double threashold;
	private final List<Simplex1D> simplices;
	private boolean runParallel;
	private boolean minimize = true;
	private static final int NMAX = 5000;
	private int iterationCount;

	public NelderMead1D(@NotNull final VCircle evalArea,
	                    @NotNull final Function<IPoint, Double> eval,
	                    final double threashold,
	                    @NotNull Collection<Pair<Double, Double>> simplices){

		this.evalArea = evalArea;
		this.eval = eval;
		this.threashold = threashold;
		this.simplices = simplices.stream().map(s -> new Simplex1D(eval, s.getLeft(), s.getRight(), evalArea, minimize)).collect(Collectors.toList());
		this.iterationCount = 0;
	}

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
		simplices.stream().forEach(s -> s.restart(evalArea.getRadius() / 10.0));

		iterationCount = 0;
		while (!hasConverged() && iterationCount < NMAX) {
			update();
			iterationCount++;
			overallIterations++;
		}
		if(iterationCount == NMAX) {
			logger.warn("max iteration reached!");
		}

		logger.info("iterations: " + overallIterations);

	}

	public VPoint getArg() {
		return getBest().getArg();
	}

	public double getValue() {
		return getBest().getValue();
	}

	public void update() {
		stream().filter(s -> !s.hasConverged(threashold)).forEach(s -> s.update());
	}

	private Simplex1D getBest() {
		Simplex1D best = null;
		for (Simplex1D simplex : simplices) {
			if(best == null || (minimize && best.getValue() > simplex.getValue()) || (!minimize && best.getValue() < simplex.getValue())) {
				best = simplex;
			}
		}

		return best;
	}

	private boolean hasConverged() {
		return stream().allMatch(s -> s.hasConverged(threashold));
	}

	private Stream<Simplex1D> stream() {
		if(runParallel) {
			return simplices.parallelStream();
		}
		else {
			return simplices.stream();
		}
	}
}
