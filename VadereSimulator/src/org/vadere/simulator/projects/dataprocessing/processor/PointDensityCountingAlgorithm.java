package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Mario Teixeira Parente
 *
 */

public class PointDensityCountingAlgorithm extends PointDensityAlgorithm {
	private double radius;
	private double circArea;

	public PointDensityCountingAlgorithm(double radius) {
		super("counting");

		this.radius = radius;
		this.circArea = this.radius * this.radius * Math.PI;
	}

	@Override
	public double getDensity(VPoint densityEvalPosition, SimulationState state) {
		int numberOfPedsInCircle = 0;

		double simTimeSeconds = state.getSimTimeInSec();

		for (Pedestrian ped : state.getTopography().getElements(Pedestrian.class)) {
			VPoint pedestrianPosition = ped.getInterpolatedFootStepPosition(simTimeSeconds);

			if (densityEvalPosition.distance(pedestrianPosition) < this.radius) {
				numberOfPedsInCircle++;
			}
		}

		return (numberOfPedsInCircle / this.circArea);
	}
}
