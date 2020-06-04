package org.vadere.simulator.models.osm.optimization;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.optimization.neldermead.NelderMead1D;
import org.vadere.util.math.optimization.neldermead.NelderMead2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class StepCircleOptimizerCircleNelderMead extends StepCircleOptimizer {

	private static Logger logger = Logger
			.getLogger(StepCircleOptimizerCircleNelderMead.class);
	private static double threshold = 0.01;

	private final Random random;
	private final AttributesOSM attributesOSM;

	public StepCircleOptimizerCircleNelderMead(@NotNull final Random random, @NotNull final AttributesOSM attributesOSM) {
		this.random = random;
		this.attributesOSM = attributesOSM;

	}

	private VPoint getNextPosition1D(@NotNull final PedestrianOSM ped, @NotNull final Shape reachableArea) {
		final VCircle stepCircle = (VCircle)reachableArea;
		Function<IPoint, Double> eval = pos -> ped.getPotential(pos);
		Collection<Pair<Double, Double>> simplices = generate1DSimplexes(ped, stepCircle);
		NelderMead1D nelderMead = new NelderMead1D(stepCircle, eval, threshold, simplices);
		nelderMead.optimize();
		return nelderMead.getArg();
	}

	@Override
	public VPoint getNextPosition(@NotNull final PedestrianOSM ped, @NotNull final Shape reachableArea) {
		VPoint point1D = getNextPosition1D(ped, reachableArea);
		VPoint point2D = getNextPosition2D(ped, reachableArea);
		double oneDimResult = ped.getPotential(point1D);
		double twoDimResult = ped.getPotential(point2D);
		double currentPotential = ped.getPotential(ped.getPosition());

		VPoint resultPoint;
		double minFuncValue;

		if(oneDimResult < twoDimResult && oneDimResult < currentPotential) {
			minFuncValue = oneDimResult;
			resultPoint = point1D;
		} else if(twoDimResult < currentPotential) {
			minFuncValue = oneDimResult;
			resultPoint = point2D;
		} else {
			minFuncValue = currentPotential;
			resultPoint = ped.getPosition();
		}

		if(getIsComputeMetric()){
			// See merge request !65
			this.computeAndAddBruteForceSolutionMetric(ped, new SolutionPair(resultPoint.clone(), minFuncValue));
		}

		return resultPoint;
	}

	private List<Pair<Double, Double>> generate1DSimplexes(@NotNull final PedestrianOSM ped, @NotNull final VCircle stepCircle) {
		int numberOfCircles = ped.getAttributesOSM().getStepCircleResolution();
		double alpha = 2*Math.PI / (2.0*numberOfCircles);
		List<Pair<Double, Double>> simplexes = new ArrayList<>(numberOfCircles);
		for(int i = 0; i < ped.getAttributesOSM().getStepCircleResolution(); i++) {
			simplexes.add(Pair.of(alpha*(2*i), alpha*(2*i+1)));
		}
		return simplexes;
	}

	private List<VTriangle> generate2DSimplexes(@NotNull final PedestrianOSM ped, @NotNull final VCircle stepCircle) {
		double step = stepCircle.getRadius() / 3.0;
		assert ped.getAttributesOSM().getNumberOfCircles() == 1;

		List<VPoint> positions = StepCircleOptimizerDiscrete.getReachablePositions(ped, stepCircle, random);
		List<VTriangle> simplices = new ArrayList<>(positions.size());
		VPoint pos = ped.getPosition();

		double angle = 2.0/3.0*Math.PI;

		VPoint dir = new VPoint(0, ped.getRadius() / 2);
		VPoint p1 = ped.getPosition().add(dir);
		VPoint p2 = ped.getPosition().add(dir.rotate(angle));
		VPoint p3 = ped.getPosition().add(dir.rotate(-angle));

		simplices.add(new VTriangle(p1, p2, p3));

		for(int i = 0; i < positions.size(); i++) {
			double innerDistance = ped.getPosition().distance((positions.get(i)));
			VPoint innerDirection = positions.get(i).subtract(pos).scalarMultiply(1.0 / innerDistance);

			int j = (i+1) % positions.size();
			VPoint anotherPoint = positions.get(j);
			double outerDistance = anotherPoint.distance((ped.getPosition()));
			VPoint outerDirection = positions.get(j).subtract(pos).scalarMultiply(1.0 / outerDistance);


			double x1 = Math.min(step, innerDistance) * innerDirection.getX();
			double y1 = Math.min(step, innerDistance) * innerDirection.getY();
			double x2 = Math.min(step, outerDistance) * outerDirection.getX();
			double y2 = Math.min(step, outerDistance) * outerDirection.getY();
			VPoint d = new VPoint(x1+x2, y1+y2).setMagnitude(step);
			p1 = pos.add(d);
			p2 = p1.add(x1, y1);
			p3 = p1.add(x2, y2);

			simplices.add(new VTriangle(p1, p2, p3));
		}

		return simplices;
	}

	//List<VPoint> positions = StepCircleOptimizerDiscrete.getReachablePositions(pedestrian, (VCircle)reachableArea, random);
	private VPoint getNextPosition2D(@NotNull final PedestrianOSM ped, @NotNull final Shape reachableArea) {

		final VCircle stepCircle = (VCircle)reachableArea;
		Function<IPoint, Double> eval = pos -> {
			if(stepCircle.contains(pos)) {
				return ped.getPotential(pos);
			}
			else {
				return NelderMead2D.MAX_VAL;
			}
		};

		/*double radius = stepCircle.getRadius() / 2;
		List<VPoint> positions = StepCircleOptimizerDiscrete.getReachablePositions(ped, new VCircle(stepCircle.getCenter(), radius), random);
		List<VTriangle> simplices = new ArrayList<>(positions.size());
		int numberOfCircles = ped.getAttributesOSM().getNumberOfCircles();
		double radDist = radius / numberOfCircles;

		for(int i = 0; i < positions.size(); i++) {
			VPoint p1 = positions.get(i);
			VPoint p2 = positions.get((i+1) % positions.size());
			double dist = p1.distance(stepCircle.getCenter());
			VPoint midPoint = new VLine(p1, p2).midPoint();
			VPoint p3;
			double circRadius = ped.getPosition().distance(p1);
			if(Math.abs(circRadius-radDist) > GeometryUtils.DOUBLE_EPS) {
				VPoint e = midPoint.subtract(ped.getPosition()).setMagnitude(circRadius-radDist);
				p3 = ped.getPosition().add(e);
			} else {
				p3 = ped.getPosition();
			}


			simplices.add(new VTriangle(p1, p2, p3));
		}



		double angle3D = 2.0/3.0*Math.PI;

		VPoint dir = new VPoint(0, ped.getRadius());
		VPoint p1 = ped.getPosition().add(dir);
		VPoint p2 = ped.getPosition().add(dir.rotate(angle3D));
		VPoint p3 = ped.getPosition().add(dir.rotate(-angle3D));

		simplices.add(new VTriangle(p1, p2, p3));*/

		List<VTriangle> simplicies = generate2DSimplexes(ped, (VCircle) reachableArea);
		NelderMead2D nelderMead = new NelderMead2D(stepCircle, eval, threshold, simplicies);
		String string = nelderMead.toString();
		//System.out.println(string);
		nelderMead.optimize();
		assert stepCircle.signedDistance(nelderMead.getArg()) < GeometryUtils.DOUBLE_EPS;
		return nelderMead.getArg();
	}

	/*private VPoint getNextPosition2D(@NotNull final PedestrianOSM ped, @NotNull final Shape reachableArea) {

		final VCircle stepCircle = (VCircle)reachableArea;
		Function<IPoint, Double> eval = pos -> {
			if(stepCircle.contains(pos)) {
				return ped.getPotential(pos);
			}
			else {
				return 10000.0;
			}
		};

		// generate start simplices
		double l = stepCircle.getRadius() * 0.5;
		double s = l * Math.sqrt(3);
		VPoint pos = ped.getPosition();
		double h = Math.sqrt(3) * 0.5 * s;
		VPoint p1 = new VPoint(0, l);
		VPoint p2 = p1.subtract(new VPoint(s * 0.5, h));
		VPoint p3 = p1.subtract(new VPoint(-s * 0.5, h));

		if(attributesOSM.isVaryStepDirection()) {
			double delta = random.nextDouble() * 2 * Math.PI;
			p1 = p1.rotate(delta);
			p2 = p2.rotate(delta);
			p3 = p3.rotate(delta);
		}

		p1 = p1.add(pos);
		p2 = p2.add(pos);
		p3 = p3.add(pos);

		Collection<VTriangle> simplices = Collections.singletonList(new VTriangle(p1, p2, p3));

		NelderMead2D nelderMead = new NelderMead2D(stepCircle, eval, attributesOSM.getMovementThreshold(), simplices);
		nelderMead.optimize();
		assert stepCircle.contains(nelderMead.getArg());
		return nelderMead.getArg();
	}*/

	@Override
	public StepCircleOptimizerCircleNelderMead clone() {
		return new StepCircleOptimizerCircleNelderMead(random, attributesOSM);
	}
}
