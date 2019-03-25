package org.vadere.util.math.optimizer.neldermead;

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
 * @author Benedikt Zoennchen
 */
public class NelderMead2D {

	private static final Logger logger = Logger.getLogger(NelderMead2D.class);
	private final VCircle evalArea;
	private final Function<IPoint, Double> eval;
	private final double threashold;
	private final List<Simplex2D> simplices;
	private boolean runParallel;
	private boolean minimize = true;
	private static final int NMAX = 5000;
	private int iterationCount;

	public NelderMead2D(@NotNull final VCircle evalArea,
	                    @NotNull final Function<IPoint, Double> eval,
	                    final double threashold,
	                    @NotNull Collection<VTriangle> simplices){

		this.evalArea = evalArea;
		this.eval = eval;
		this.threashold = threashold;
		this.simplices = simplices.stream().map(triangle -> new Simplex2D(eval, triangle, minimize)).collect(Collectors.toList());
		this.iterationCount = 0;
	}

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
		simplices.stream().forEach(s -> s.restart(evalArea.getRadius() / 10.0));

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

		//logger.info("iterations: " + overallIterations);
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

	private Simplex2D getBest() {
		Simplex2D best = null;
		for (Simplex2D simplex : simplices) {
			if(best == null || (minimize && best.getValue() > simplex.getValue()) || (!minimize && best.getValue() < simplex.getValue())) {
				best = simplex;
			}
		}

		return best;
	}

	private boolean hasConverged() {
		return stream().allMatch(s -> s.hasConverged(threashold));
	}

	private Stream<Simplex2D> stream() {
		if(runParallel) {
			return simplices.parallelStream();
		}
		else {
			return simplices.stream();
		}
	}
}
