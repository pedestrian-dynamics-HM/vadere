package org.vadere.simulator.projects.dataprocessing.processors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;

import com.google.gson.annotations.Expose;

/**
 * Adds information of an pedestrian of the simulation to the table.
 * 
 * <p>
 * <b>Added column names</b>: id {@link Integer}, lastX {@link Double}, lastY {@link Double},
 * targetId {@link Integer}, sourceId {@link Integer},
 * firstX {@link Double}, firstY {@link Double}, startTime {@link Double}, endTime {@link Double},
 * evacTime {@link Double}
 * </p>
 * 
 *
 */
public class PedestrianLastPositionProcessor extends AbstractProcessor {

	@Expose
	private Table table;

	@Expose
	private Map<Integer, VPoint> lastKnownPositions;

	@Expose
	private Map<Integer, VPoint> firstPositions;

	@Expose
	private Map<Integer, Double> endTimes;

	@Expose
	private Map<Integer, Double> startTimes;

	@Expose
	private Map<Integer, Integer> targetIds;

	@Expose
	private Map<Integer, Integer> sourceIds;

	@Expose
	private double meanEvacuationTime;

	public PedestrianLastPositionProcessor() {
		super(new Table("time", "id", "lastX", "lastY", "firstX", "firstY", "targetId", "sourceId", "startTime",
				"endTime", "evacTime"));
		table = getTable();
		lastKnownPositions = new HashMap<>();
		firstPositions = new HashMap<>();
		endTimes = new HashMap<>();
		startTimes = new HashMap<>();
		targetIds = new HashMap<>();
		sourceIds = new HashMap<>();
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();

		/*
		 * //TODO: [priority=medium] [task=refactoring]
		 * This is simple but expensive, maybe we should use the remove and
		 * add pedestrian listener in Topography. On the other hand processors
		 * act on the state (topography and so on) and the state should not
		 * act on the processors.
		 * 
		 * This processor could be a PedestranRemoveListener, in that case the
		 * processor should get the original Topography in the Constructor and
		 * register himself to it.
		 */
		for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
			VPoint pedPos = entry.getValue();
			int pedId = entry.getKey();

			lastKnownPositions.put(pedId, pedPos);

			if (!startTimes.containsKey(pedId)) {
				startTimes.put(pedId, state.getSimTimeInSec());
				firstPositions.put(pedId, pedPos);
			}

			Pedestrian p = state.getTopography().getElement(Pedestrian.class, pedId);
			Car c = state.getTopography().getElement(Car.class, pedId);

			List<Integer> targets = null;
			if (p != null)
				targets = p.getTargets();
			else if (c != null)
				targets = c.getTargets();
			else
				throw new IllegalArgumentException(
						"PedestrianLastPositionProcessor cannot deal with elements that are neither cars nor pedestrians.");

			if (targets.size() > 0) {
				targetIds.put(pedId, targets.get(0));
			} else {
				targetIds.put(pedId, -1);
			}
			// TODO [priority=medium] [task=feature] add source id. source ID is not yet stored in a pedestrian
			sourceIds.put(pedId, -1);

			endTimes.put(pedId, state.getSimTimeInSec());
		}

		return super.postUpdate(state);
	}

	@Override
	public Table postLoop(final SimulationState state) {

		if (table.isEmpty()) {
			double evacTimeSum = 0;
			for (Integer pedId : lastKnownPositions.keySet()) {
				VPoint lastPedPos = lastKnownPositions.get(pedId);
				VPoint firstPedPos = firstPositions.get(pedId);
				table.addRow();
				table.addColumnEntry("time", state.getSimTimeInSec());
				table.addColumnEntry("id", pedId);
				table.addColumnEntry("lastX", lastPedPos.x);
				table.addColumnEntry("lastY", lastPedPos.y);
				table.addColumnEntry("targetId", targetIds.get(pedId));
				table.addColumnEntry("sourceId", sourceIds.get(pedId));
				table.addColumnEntry("firstX", firstPedPos.x);
				table.addColumnEntry("firstY", firstPedPos.y);
				table.addColumnEntry("startTime", startTimes.get(pedId));
				table.addColumnEntry("endTime", endTimes.get(pedId));

				double evacTime = endTimes.get(pedId) - startTimes.get(pedId);
				evacTimeSum += evacTime;
				table.addColumnEntry("evacTime", evacTime);
			}

			meanEvacuationTime = evacTimeSum / lastKnownPositions.size();
		}
		return table;

	}

	protected double getMeanEvactuationTime() {
		return meanEvacuationTime;
	}

	@Override
	public PedestrianLastPositionProcessor clone() {
		return new PedestrianLastPositionProcessor();
	}
}
