package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.density.IGaussianFilter;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Mario Teixeira Parente
 *
 */

public class PointDensityGaussianAlgorithm extends PointDensityAlgorithm {
	private double scale;
	private double standDev;
	private boolean isObstacleDensity;

	private IGaussianFilter obstacleFilter;
	private IGaussianFilter pedestrianFilter;

	private boolean filteredObstacles;

	private int lastStep;

	public PointDensityGaussianAlgorithm(double scale, double standDev, boolean isObstacleDensity) {
		super("gaussian" + scale);

		this.scale = scale;
		this.standDev = standDev;
		this.isObstacleDensity = isObstacleDensity;

		this.filteredObstacles = false;

		this.lastStep = 0;
	}

	@Override
	public double getDensity(final VPoint pos, final SimulationState state) {
		if (state.getStep() > this.lastStep) {
			// obstacle will not change so we do it once.
			if (!this.filteredObstacles && this.isObstacleDensity) {
				this.obstacleFilter = IGaussianFilter.create(
						state.getTopography(),
						this.scale,
						this.standDev);
				this.obstacleFilter.filterImage();
				this.filteredObstacles = true;
			}

			this.pedestrianFilter = IGaussianFilter.create(
					state.getTopography().getBounds(),
					state.getTopography().getElements(Pedestrian.class),
					this.scale,
					this.standDev,
					state.getTopography().getAttributesPedestrian(),
					IPedestrianLoadingStrategy.create());
			this.pedestrianFilter.filterImage();

			this.lastStep = state.getStep();
		}

		double density = 0.0;
		if (this.isObstacleDensity)
			density = this.obstacleFilter.getFilteredValue(pos.x, pos.y);

		density += this.pedestrianFilter.getFilteredValue(pos.x, pos.y);

		return density;
	}
}
