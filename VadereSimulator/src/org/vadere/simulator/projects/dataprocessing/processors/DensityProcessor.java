package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * Adds the current timeStep (step), the current time, position (x, y-coordinates) of each position
 * in the position list of this Processor and additional adds the density on these position.
 * 
 * <p>
 * <b>Added column names</b>: step {@link Integer}, time {@link Double}, x {@link Double}, y
 * {@link Double}
 * </p>
 *
 *
 */
public abstract class DensityProcessor extends AbstractProcessor {

	@Expose
	private List<VPoint> positions;

	@Expose
	private int lastStep;

	public DensityProcessor() {
		getTable().clear("step", "time", "x", "y", getDensityType());
		positions = new ArrayList<>();
		this.lastStep = 0;
	}

	@Override
	public Table preLoop(SimulationState state) {
		return super.preLoop(state);
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		Table table = getTable();
		if (table.isEmpty() || state.getStep() != lastStep) {
			table.clear();

			for (int row = 0; row < getPositions().size(); row++) {
				VPoint position = getPositions().get(row);
				table.addRow();
				table.addColumnEntry("step", state.getStep());
				table.addColumnEntry("time", state.getSimTimeInSec());
				table.addColumnEntry("x", position.x);
				table.addColumnEntry("y", position.y);
				if (isColumnVisible(getDensityType())) {
					table.addColumnEntry(getDensityType(), getDensity(position, state));
				}

			}
		}

		return table;
	}

	/**
	 * Every DensityProcessor should has a unique density type (the name of the column in the
	 * table).
	 * This method returns the unique name to identify the column of this density.
	 * 
	 * @return the unique name to identify the column of this density
	 */
	public abstract String getDensityType();

	/**
	 * Returns the density of a specific position for the given simulation state.
	 * 
	 * @param position the position where the measurement will take place
	 * @param state the simulation state
	 * @return the density of a specific position
	 */
	protected abstract double getDensity(final VPoint position, final SimulationState state);

	protected List<VPoint> getPositions() {
		return positions;
	}

	public void addAllPositions(final Collection<? extends VPoint> positions) {
		this.positions.addAll(positions);
	}

	public void addAllPositions(final VPoint... positions) {
		addAllPositions(Arrays.asList(positions));
	}

	public void addPosition(final VPoint position) {
		positions.add(position);
	}

	public void removePosition(final VPoint position) {
		positions.remove(position);
	}

	public void removeAllPositions() {
		positions.clear();
	}

	@Override
	public abstract DensityProcessor clone();

	@Override
	public boolean equals(final Object obj) {
		if (super.equals(obj)) {
			DensityProcessor tmp = (DensityProcessor) obj;
			return tmp.positions.equals(positions);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + positions.hashCode();
		result = 47 * result + lastStep;
		return result;
	}
}
