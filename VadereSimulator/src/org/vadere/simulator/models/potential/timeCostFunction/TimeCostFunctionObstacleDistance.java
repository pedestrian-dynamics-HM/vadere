package org.vadere.simulator.models.potential.timeCostFunction;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.potential.solver.timecost.ITimeCostFunction;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Function;

public class TimeCostFunctionObstacleDistance implements ITimeCostFunction {

	private final transient ITimeCostFunction timeCostFunction;
	private final transient Function<IPoint, Double> obstacleDistanceFunction;
	private final double height;
	private final double width;

	public TimeCostFunctionObstacleDistance(
			@NotNull final ITimeCostFunction timeCostFunction,
			@NotNull final Function<IPoint, Double> obstacleDistanceFunction,
			final double height,
			final double width) {
		this.timeCostFunction = timeCostFunction;
		this.obstacleDistanceFunction = obstacleDistanceFunction;
		this.height = height;
		this.width = width;
	}

	@Override
	public double costAt(IPoint p) {
		double timeCost = timeCostFunction.costAt(p);
		double distance = obstacleDistanceFunction.apply(p);

		if(distance <= 0) {
			timeCost += height;
		}
		else if(distance > 0 && distance < width) {
			//timeCost += height * Math.exp(2 / (Math.pow(distance / (width), 2) - 1));
			timeCost += (1-(distance/ width)) * height;
		}
		return timeCost;
	}

	@Override
	public void update() {
		timeCostFunction.update();
	}

	@Override
	public boolean needsUpdate() {
		return timeCostFunction.needsUpdate();
	}

	@Override
	public String toString() {
		return "(obstacle distance function(x)) + " + timeCostFunction;
	}
}
