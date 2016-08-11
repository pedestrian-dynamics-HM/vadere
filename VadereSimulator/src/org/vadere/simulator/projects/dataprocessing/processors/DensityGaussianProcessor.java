package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.density.IGaussianFilter;
import org.vadere.simulator.models.potential.timeCostFunction.loading.IPedestrianLoadingStrategy;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.processors.AttributesDensityGaussianProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * Adds the current timeStep (step), the current time, position (x, y-coordinates) of each position
 * in the position list of this Processor and additional adds the density on these position.
 * The density is calculated by using the gaussian filter (image processing technique).
 * 
 * <p>
 * Note: This processor does only work online, since it needs real pedestrain objects
 * </p>
 * 
 * <p>
 * <b>Added column names</b>: step {@link Integer}, time {@link Double}, x {@link Double}, y
 * {@link Double}, gaussianDensity {@link Double}
 * </p>
 *
 *
 */
public class DensityGaussianProcessor extends DensityProcessor {

	@Expose
	private static Logger logger = LogManager.getLogger(DensityGaussianProcessor.class);

	@Expose
	private final Table table;

	@Expose
	private int lastStep;

	@Expose
	private boolean filteredObstacles;

	@Expose
	private IGaussianFilter pedestrianFilter;

	@Expose
	private IGaussianFilter obstacleFilter;

	private AttributesDensityGaussianProcessor attributes;

	public DensityGaussianProcessor(final AttributesDensityGaussianProcessor attributes) {
		super();
		this.attributes = attributes;
		table = getTable();
		this.lastStep = 0;
		this.filteredObstacles = false;
	}

	public DensityGaussianProcessor() {
		this(new AttributesDensityGaussianProcessor());
	}

	@Override
	public Table preLoop(SimulationState state) {
		return super.preLoop(state);
	}

	@Override
	protected double getDensity(final VPoint position, final SimulationState state) {
		double density = 0.0;
		if (lastStep != state.getStep()) {
			// obstacle will not change so we do it once.
			if (!filteredObstacles && attributes.isObstacleDensity()) {
				this.obstacleFilter = IGaussianFilter.create(
						state.getTopography(),
						attributes.getScale(),
						attributes.getStandardDerivation());
				this.obstacleFilter.filterImage();
				filteredObstacles = true;
			}


			this.pedestrianFilter = IGaussianFilter.create(
					state.getTopography().getBounds(),
					state.getTopography().getElements(Pedestrian.class),
					attributes.getScale(),
					attributes.getStandardDerivation(),
					state.getTopography().getAttributesPedestrian(),
					IPedestrianLoadingStrategy.create());
			this.pedestrianFilter.filterImage();
		}

		if (attributes.isObstacleDensity()) {
			density = this.obstacleFilter.getFilteredValue(position.x, position.y);
		}
		density += this.pedestrianFilter.getFilteredValue(position.x, position.y);

		this.lastStep = state.getStep();
		return density;
	}

	@Override
	public DensityProcessor clone() {
		return new DensityGaussianProcessor();
	}

	@Override
	public String getDensityType() {
		return "gaussianDensity";
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}
}
