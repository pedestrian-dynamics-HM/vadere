package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.processors.AttributesPedestrianWaitingTimeProcessor;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;


/**
 * Computes the total time a pedestrian spends on a given area during the simulation.
 * If the time is zero, nothing is reported.
 * 
 * <p>
 * <b>Added column names</b>: id {@link Integer}, time {@link Double}, totalWaitingTime
 * {@link Double}
 * </p>
 * 
 *
 */
public class PedestrianWaitingTimeProcessor extends AbstractProcessor {

	private static class WaitingTimeData {
		public double waitingTime;
		public double endTime;

		public WaitingTimeData(double waitingTime, double endTime) {
			this.waitingTime = waitingTime;
			this.endTime = endTime;
		}
	}

	@Expose
	private Table table;

	@Expose
	private final Map<Integer, WaitingTimeData> waitingTimes;

	@Expose
	private double lastSimTimeInSec;

	@Expose
	private double meanEvacuationTime;

	private AttributesPedestrianWaitingTimeProcessor attributes;

	public PedestrianWaitingTimeProcessor(final AttributesPedestrianWaitingTimeProcessor attributes) {
		super(new Table("time", "id", "totalWaitingTime"));
		table = getTable();
		waitingTimes = new HashMap<>();
		this.attributes = attributes;
	}

	public PedestrianWaitingTimeProcessor() {
		this(new AttributesPedestrianWaitingTimeProcessor());
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	@Override
	public Table preLoop(SimulationState state) {
		lastSimTimeInSec = state.getSimTimeInSec();
		return super.preLoop(state);
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();

		double dt = state.getSimTimeInSec() - lastSimTimeInSec;

		for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
			VPoint pedPos = entry.getValue();
			int pedId = entry.getKey();

			if (!waitingTimes.containsKey(pedId)) {
				waitingTimes.put(pedId, new WaitingTimeData(0.0, state.getSimTimeInSec()));
			}

			if (attributes.getWaitingArea().contains(pedPos)) {
				WaitingTimeData data = waitingTimes.get(pedId);
				data.waitingTime = waitingTimes.get(pedId).waitingTime + dt;
				data.endTime = state.getSimTimeInSec();
			}
		}

		lastSimTimeInSec = state.getSimTimeInSec();

		return super.postUpdate(state);
	}

	@Override
	public Table postLoop(final SimulationState state) {

		if (table.isEmpty()) {
			for (Integer pedId : waitingTimes.keySet()) {
				table.addRow();

				WaitingTimeData data = waitingTimes.get(pedId);

				table.addColumnEntry("time", data.endTime);
				table.addColumnEntry("id", pedId);
				table.addColumnEntry("totalWaitingTime", data.waitingTime);
			}
		}
		return table;

	}

	@Override
	public PedestrianWaitingTimeProcessor clone() {
		return new PedestrianWaitingTimeProcessor(attributes);
	}
}
