package org.vadere.simulator.projects.dataprocessing.processors;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.processors.AttributesDensityCountingProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;

import com.google.gson.annotations.Expose;

/**
 * Adds the current timeStep (step), the current time, position (x, y-coordinates) of each position
 * in the position list of this Processor and additional adds the density on these position.
 * The density is calculated by counting pedestrians in a circle around each position.
 * 
 * <p>
 * <b>Added column names</b>: step {@link Integer}, time {@link Double}, x {@link Double}, y
 * {@link Double}, circleDensity {@link Double}
 * </p>
 *
 *
 */
public class DensityCountingProcessor extends DensityProcessor {

	private AttributesDensityCountingProcessor attributes;

	@Expose
	private Table table;

	@Expose
	private double circleArea;

	public DensityCountingProcessor() {
		this(new AttributesDensityCountingProcessor());
	}

	public DensityCountingProcessor(final AttributesDensityCountingProcessor attributes) {
		this.attributes = attributes;
		this.circleArea = attributes.getRadius() * attributes.getRadius() * Math.PI;
		table = getTable();
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	protected double getDensity(final VPoint position, final SimulationState state) {
		int numberOfPedsInCircle = 0;
		for (Pedestrian ped : state.getTopography().getElements(Pedestrian.class)) {
			if (position.distance(ped.getPosition()) < attributes.getRadius()) {
				numberOfPedsInCircle++;
			}
		}

		return (numberOfPedsInCircle / circleArea);
	}

	@Override
	public DensityCountingProcessor clone() {
		return new DensityCountingProcessor(attributes);
	}

	@Override
	public boolean equals(final Object obj) {
		if (super.equals(obj)) {
			DensityCountingProcessor tmp = (DensityCountingProcessor) obj;
			return tmp.attributes.equals(attributes);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		long temp;
		result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
		result = 31 * result + (table != null ? table.hashCode() : 0);
		temp = Double.doubleToLongBits(circleArea);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String getDensityType() {
		return "circleDensity";
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}
}
