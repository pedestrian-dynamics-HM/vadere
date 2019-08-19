package org.vadere.util.math.optimization;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PatternSearch {
	private static Logger log = Logger.getLogger(PatternSearch.class);
	private final double distanceThreshold;
	private final Function<VPoint, Double> eval;
	private final VCircle evalArea;
	private double globalLen;
	private VPoint globalBase;
	private VCircle normCircle;
	private boolean globalSearch = true;
	private final List<VPoint> basePoints;
	private int numberOfCircles;
	private int numberOfPoints;
	private int nShink;
	private int starts = 3;
	private List<LocalOptimizer> localOptimizers;
	private int evalCounter;


	public PatternSearch(@NotNull final VCircle evalArea,
	                     @NotNull final Function<VPoint, Double> eval,
	                     final double distanceThreshold,
	                     @NotNull final List<VPoint> basePoints,
	                     int numberOfCircle,
	                     int numberOfPoints) {
		this.distanceThreshold = distanceThreshold;
		this.eval = eval;
		this.evalArea = evalArea;
		this.globalLen = evalArea.getRadius();
		this.globalBase = new VPoint(0, 0);
		this.normCircle = new VCircle(globalBase, evalArea.getRadius());
		this.basePoints = basePoints.stream().map(p -> p.scalarMultiply(evalArea.getRadius())).collect(Collectors.toList());
		this.numberOfCircles = numberOfCircle;
		this.numberOfPoints = numberOfPoints;
		this.nShink = 0;
		this.localOptimizers = new ArrayList<>(starts);
	}

	public PatternSearch(@NotNull final VCircle evalArea,
	                     @NotNull final Function<VPoint, Double> eval,
	                     final double distanceThreshold,
	                     @NotNull final List<VPoint> basePoints,
	                     double edgeLen) {
		this.distanceThreshold = distanceThreshold;
		this.eval = eval;
		this.evalArea = evalArea;
		this.globalLen = edgeLen;
		this.globalBase = new VPoint(0, 0);
		this.normCircle = new VCircle(globalBase, evalArea.getRadius());
		this.basePoints = basePoints.stream().map(p -> p.scalarMultiply(evalArea.getRadius())).collect(Collectors.toList());
		//log.info(basePoints.size());
		this.numberOfCircles = 0;
		this.numberOfPoints = 0;
		this.localOptimizers = new ArrayList<>(starts);

	}

	public void optimize() {
		if(globalSearch) {
			globalSearch();
			evalCounter = basePoints.size();
		} else {
			localOptimizers.add(new LocalOptimizer(globalBase, evalArea.getRadius()));
		}

		boolean updateRequired = true;
		while (updateRequired) {
			updateRequired = false;
			for(LocalOptimizer localOptimizer : localOptimizers) {
				updateRequired = updateRequired || localOptimizer.update();
			}
		}
	}

	public double getMinValue() {
		return localOptimizers.stream().mapToDouble(localOptimizer -> localOptimizer.getMinValue()).min().getAsDouble();
	}

	public VPoint getGlobalBase() {
		double minValue = Double.MAX_VALUE;
		VPoint base = normCircle.getCenter();
		for(LocalOptimizer localOptimizer : localOptimizers) {
			if(minValue > localOptimizer.getMinValue()) {
				base = localOptimizer.getBase();
				minValue = localOptimizer.getMinValue();
			}
		}

		return base;
	}

	public VPoint getArgMin() {
		//log.info("evalCounter = " + evalCounter);
		return getGlobalBase().add(evalArea.getCenter());
	}

	private void globalSearch() {
		PriorityQueue<Pair<VPoint, Double>> list = new PriorityQueue<>(Comparator.comparingDouble(p -> -p.getValue()));
		for(VPoint evalPoint : basePoints) {
			double evaluation = eval(evalPoint);
			if(list.size() >= starts && evaluation < list.peek().getSecond()) {
				list.poll();
			}
			else if(list.size() < starts){
				list.add(Pair.create(evalPoint, evaluation));
			}
		}
		globalLen = Math.min(globalLen, Math.max(evalArea.getRadius() / (numberOfCircles > 0 ? numberOfCircles : 1), 2 * Math.PI * evalArea.getRadius() / (numberOfPoints > 0 ? numberOfPoints : 1)));

		for(Pair<VPoint, Double> pair : list) {
			localOptimizers.add(new LocalOptimizer(pair.getFirst(), evalArea.getRadius() / 8));
		}
	}

	private boolean contains(@NotNull final VPoint point) {
		return normCircle.getRadius() * normCircle.getRadius() >= normCircle.getCenter().distanceSq(point) - GeometryUtils.DOUBLE_EPS;
	}

	private boolean onCircle(@NotNull final VPoint point) {
		return Math.abs(normCircle.getRadius() * normCircle.getRadius() - normCircle.getCenter().distanceSq(point)) < GeometryUtils.DOUBLE_EPS;
	}


	private double eval(final double x, final double y) {
		evalCounter++;
		return eval.apply(evalArea.getCenter().add(x, y));
	}

	private double eval(final VPoint point) {
		evalCounter++;
		return eval.apply(evalArea.getCenter().add(point));
	}

	private class LocalOptimizer {
		private VPoint[] crossPoints;
		private VPoint base;
		private int nPoints = 4;
		private double minValue;
		private double len;
		private int nShrink;
		private int iteration;

		private LocalOptimizer(@NotNull final VPoint base, final double len) {
			this.crossPoints = new VPoint[nPoints];
			this.iteration = 0;
			this.minValue = eval(base);
			this.base = base;
			this.nShrink = 2;
			this.len = len;
			recomputeCrossPoints();
		}

		private boolean update() {
			if(len > distanceThreshold) {
				if(!explore()) {
					shrink();
				}
				recomputeCrossPoints();
				iteration++;
				return true;
			}
			return false;
		}

		private void recomputeCrossPoints() {
			crossPoints[0] = new VPoint(len, 0);
			crossPoints[1] = new VPoint(-len, 0);
			crossPoints[2] = new VPoint(0, len);
			crossPoints[3] = new VPoint(0, -len);
			// globalBase on circle center.distance(pos) - this.radius

			// globalBase is on the circle
			if(onCircle(base)) {
				//log.info("onCircle");
				for(int i = 0; i < crossPoints.length; i++) {
					VPoint crossPoint = base.add(crossPoints[i]);
					if(!contains(crossPoint)) {
						VPoint rotatedCrossPoint = base.rotate((crossPoints[i].x + crossPoints[i].y) / normCircle.getRadius()).subtract(base);
						crossPoints[i] = rotatedCrossPoint;
					}
				}
			} else {
				//log.info("not onCircle");
				for(int i = 0; i < crossPoints.length; i++) {
					VPoint crossPoint = base.add(crossPoints[i]);
					if(!contains(crossPoint)) {
						Optional<VPoint> intersectionPoint = normCircle.getSegmentIntersectionPoints(crossPoint.x, crossPoint.y, base.x, base.y);
						if(intersectionPoint.isPresent()) {
							crossPoints[i] = intersectionPoint.get().subtract(base);
						}
					}
				}
			}
			/*if(normCircle.contains(globalBase)) {
				for(int i = 0; i < crossPoints.length; i++) {
					VPoint crossPoint = globalBase.add(crossPoints[i]);
					if(!normCircle.contains(crossPoint)) {
						Optional<VPoint> intersectionPoint = normCircle.getSegmentIntersectionPoints(crossPoint.x, crossPoint.y, globalBase.x, globalBase.y);
						if(intersectionPoint.isPresent()) {
							crossPoints[i] = intersectionPoint.get().subtract(globalBase);
						}
					}
				}
			}*/
		}

		public double getMinValue() {
			return minValue;
		}

		public VPoint getArgMin() {
			return getBase().add(evalArea.getCenter());
		}

		public VPoint getBase() {
			return base;
		}

		private void shrink() {
			len /= nShrink;
			nShrink++;
		}

		private boolean explore() {
			boolean success = false;
			VPoint minArg = null;
			for(VPoint point : crossPoints) {
				VPoint evalPoint = point.add(base);
				//if(contains(evalPoint)) {
					double evaluation = eval(evalPoint);
					if(evaluation < minValue) {
						minValue = evaluation;
						minArg = evalPoint;
						success = true;
					}
			//	}
			}

			if(success) {
				base = minArg;
			}

			return success;
		}

	}

}
