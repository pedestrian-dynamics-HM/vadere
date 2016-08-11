package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.data.Row;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Adds information of all pedestrian to the table, at the end of the simulation.
 * 
 * <p>
 * <b>Added column names</b>: id {@link Integer}, x {@link Double}, y
 * {@link Double}, targetId {@link Integer}, sourceId {@link Integer}, vx
 * {@link Double}, vy {@link Double}, desiredSpeed {@link Double}, time
 * {@link Double}
 * </p>
 * 
 * 
 */
public class PedestrianAttributesProcessor extends AbstractProcessor {

	@Expose
	private static Logger logger = LogManager.getLogger(PedestrianAttributesProcessor.class);

	@Expose
	private Table table;

	@Expose
	private Map<Integer, Pedestrian> pedestrians;

	/** Used to compute velocities during the simulation to be able to expose them at the end. */
	private PedestrianVelocityProcessor velocityProcessor;
	private Table lastVelocityTable;

	public PedestrianAttributesProcessor() {
		super(new Table("time", "id", "lastX", "lastY", "lastVX", "lastVY", "targetId", "sourceId", "desiredSpeed"));
		table = getTable();

		pedestrians = new HashMap<>();
		velocityProcessor = new PedestrianVelocityProcessor();
		velocityProcessor.addColumnNames("vx", "vy");

		lastVelocityTable = new Table("lastVX", "lastVY");
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	@Override
	public Table preLoop(SimulationState state) {
		velocityProcessor.preLoop(state);
		return super.preLoop(state);
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();

		for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
			int pedId = entry.getKey();
			Pedestrian ped = state.getTopography().getElement(Pedestrian.class, pedId);

			pedestrians.put(pedId, ped);
		}

		lastVelocityTable = velocityProcessor.postUpdate(state);

		return super.postUpdate(state);
	}

	@Override
	public Table postLoop(final SimulationState state) {

		Map<Integer, Integer> velocityRows = new HashMap<>();
		try {
			Iterator<Row> it = lastVelocityTable.iterator();
			int rowCounter = 0;
			for (; it.hasNext();) {
				Row row = it.next();
				velocityRows.put(Integer.parseInt(row.getEntry("id").toString()), rowCounter++);
			}
		} catch (Exception e) {
			logger.error(e);
		}

		if (table.isEmpty()) {
			for (Entry<Integer, Pedestrian> id_ped : pedestrians.entrySet()) {
				Pedestrian ped = id_ped.getValue();
				int id = id_ped.getKey();

				table.addRow();
				table.addColumnEntry("time", state.getSimTimeInSec());
				table.addColumnEntry("id", id);
				table.addColumnEntry("lastX", ped.getPosition().x);
				table.addColumnEntry("lastY", ped.getPosition().y);
				if (velocityRows.containsKey(id)) {
					int rowId = velocityRows.get(id);
					table.addColumnEntry("lastVX", lastVelocityTable.getEntry("vx", rowId));
					table.addColumnEntry("lastVY", lastVelocityTable.getEntry("vy", rowId));
				} else {
					table.addColumnEntry("lastVX", 0.0);
					table.addColumnEntry("lastVY", 0.0);
				}
				if (ped.hasNextTarget())
					table.addColumnEntry("targetId", ped.getNextTargetId());
				else
					table.addColumnEntry("targetId", -1);
				// TODO [priority=medium] [task=feature] add source id
				table.addColumnEntry("sourceId", -1);
				table.addColumnEntry("desiredSpeed", ped.getFreeFlowSpeed());
			}
		}
		return table;

	}

	@Override
	public PedestrianLastPositionProcessor clone() {
		return new PedestrianLastPositionProcessor();
	}
}
