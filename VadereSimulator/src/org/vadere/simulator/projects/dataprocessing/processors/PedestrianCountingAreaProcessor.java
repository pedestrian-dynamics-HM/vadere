package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.processors.AttributesPedestrianWaitingTimeProcessor;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;


/**
 * Computes the total number of pedestrians on a given area during the simulation, in each second.
 * 
 * <p>
 * <b>Added column names</b>: time {@link Double}, count {@link Integer}
 * </p>
 * 
 *
 */
public class PedestrianCountingAreaProcessor extends AbstractProcessor {


	@Expose
	private Table table;

	private AttributesPedestrianWaitingTimeProcessor attributes;

	private int lastStep;

	public PedestrianCountingAreaProcessor(final AttributesPedestrianWaitingTimeProcessor attributes) {
		super(new Table("time", "count"));
		table = getTable();
		this.attributes = attributes;
	}

	public PedestrianCountingAreaProcessor() {
		this(new AttributesPedestrianWaitingTimeProcessor());
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	@Override
	public Table preLoop(SimulationState state) {
		this.lastStep = 0;
		return super.preLoop(state);
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();

		int count = 0;
		for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
			VPoint pedPos = entry.getValue();

			if (attributes.getWaitingArea().contains(pedPos)) {
				count++;
			}
		}

		if (table.isEmpty() || state.getStep() != lastStep) {
			table.clear();
			table.addRow();
			table.addColumnEntry("time", state.getSimTimeInSec());
			table.addColumnEntry("count", count);
		}

		lastStep = state.getStep();
		return table;
	}

	@Override
	public Table postLoop(final SimulationState state) {
		return table;
	}

	@Override
	public PedestrianCountingAreaProcessor clone() {
		return new PedestrianCountingAreaProcessor(attributes);
	}
}
