package org.vadere.simulator.models.osm.optimization;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.util.geometry.Geometry;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.optimizer.neldermead.NelderMead1D;
import org.vadere.util.math.optimizer.neldermead.NelderMead2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class StepCircleOptimizerCircleNelderMead implements StepCircleOptimizer {

	private static Logger logger = Logger
			.getLogger(StepCircleOptimizerCircleNelderMead.class);

	private final Random random;
	private final AttributesOSM attributesOSM;

	public StepCircleOptimizerCircleNelderMead(@NotNull final Random random, @NotNull final AttributesOSM attributesOSM) {
		this.random = random;
		this.attributesOSM = attributesOSM;

	}

	private VPoint getNextPosition1D(@NotNull final PedestrianOSM ped, @NotNull final Shape reachableArea) {
		final VCircle stepCircle = (VCircle)reachableArea;
		Function<IPoint, Double> eval = pos -> ped.getPotential(pos);
		Collection<Pair<Double, Double>> simplices = Collections.singletonList(Pair.of(0.0, Math.PI));
		NelderMead1D nelderMead = new NelderMead1D(stepCircle, eval, attributesOSM.getMovementThreshold(), simplices);
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

		if(oneDimResult < twoDimResult && oneDimResult < currentPotential) {
			return point1D;
		} else if(twoDimResult < currentPotential) {
			return point2D;
		} else {
			return ped.getPosition();
		}
		//return point2D;
	}

	//List<VPoint> positions = StepCircleOptimizerDiscrete.getReachablePositions(pedestrian, (VCircle)reachableArea, random);
	private VPoint getNextPosition2D(@NotNull final PedestrianOSM ped, @NotNull final Shape reachableArea) {

		final VCircle stepCircle = (VCircle)reachableArea;
		Function<IPoint, Double> eval = pos -> {
			if(stepCircle.contains(pos)) {
				return ped.getPotential(pos);
			}
			else {
				return 10000.0;
			}
		};

		List<VPoint> positions = StepCircleOptimizerDiscrete.getReachablePositions(ped, stepCircle, random);
		List<VTriangle> simplices = new ArrayList<>(positions.size());
		int numberOfCircles = ped.getAttributesOSM().getNumberOfCircles();
		double radDist = stepCircle.getRadius() / numberOfCircles;
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

		NelderMead2D nelderMead = new NelderMead2D(stepCircle, eval, attributesOSM.getMovementThreshold(), simplices);
		nelderMead.optimize();
		assert stepCircle.contains(nelderMead.getArg());
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
