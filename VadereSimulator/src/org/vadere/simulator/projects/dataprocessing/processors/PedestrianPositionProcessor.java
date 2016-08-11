package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import java.util.*;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.processors.AttributesPedestrianPositionProcessor;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.Step;
import org.vadere.util.data.Row;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.io.IOUtils;

/**
 * Adds the current timeStep (step), the current time, the id and the position (x, y-coordinates) of
 * each
 * pedestrian in the {@link SimulationState} to the table. It is also possible to add a list of
 * processors
 * that generate a Row for each pedestrian position (for each time step). This makes it possible to
 * combine
 * for example a PedestrianDensityProcessor and a PedestrianVelocityProcessor.
 * 
 * <p>
 * <b>Added column names</b>: step {@link Integer}, time {@link Double}, id {@link Integer}, x
 * {@link Double}, y {@link Double}, targetId {@link Integer}, sourceId {@link Integer}
 * </p>
 *
 *
 */
public class PedestrianPositionProcessor extends AbstractProcessor implements ForEachPedestrianPositionProcessor {

	private AttributesPedestrianPositionProcessor attributes;

	@Expose
	private int lastStep;

	@Expose
	private Table table;

	@Expose
	private Map<Integer, VPoint> positionMap;

	public PedestrianPositionProcessor() {
		this(new AttributesPedestrianPositionProcessor());
	}

	public PedestrianPositionProcessor(final AttributesPedestrianPositionProcessor attributes) {
		super(new Table("step", "time", "id", "x", "y", "targetId", "sourceId"));
		this.attributes = attributes;
		table = getTable();
		positionMap = new HashMap<>();
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	@Override
	public Row postUpdate(final SimulationState state, int pedId, VPoint position) {

		int sId = -1; // //TODO: [priority=medium] [task=feature] add source id
		int tId = -1;
		Pedestrian p = state.getTopography().getElement(Pedestrian.class, pedId);
		if (p != null && p.hasNextTarget()) {
			tId = state.getTopography().getElement(Pedestrian.class, pedId).getNextTargetId();
		}

		Row row = new Row("x", "step", "time", "id", "x");
		row.setEntry("step", state.getStep());
		row.setEntry("time", state.getSimTimeInSec());
		row.setEntry("id", pedId);
		row.setEntry("x", position.x);
		row.setEntry("y", position.y);
		row.setEntry("targetId", tId);
		row.setEntry("sourceId", sId);
		positionMap.put(pedId, position);

		return row;
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		if (lastStep == 0) {
			positionMap.clear();
		}

		if (table.isEmpty() || state.getStep() != lastStep) {
			Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();
			table.clear();

			Set<Integer> pedIds = pedPosMap.keySet();

			for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
				int pedId = entry.getKey();
				VPoint position = entry.getValue();

				/**
				 * This line is required to write only position if the position changes.
				 */
				if (!attributes.isIgnoreEqualPositions()
						|| !position.equals(positionMap.get(pedId), GeometryUtils.DOUBLE_EPS)) {
					Row row = postUpdate(state, pedId, position);
					table.addRow(row);
				}
			}
		}

		lastStep = state.getStep();
		return table;
	}

	protected Map<Integer, VPoint> getPositions() {
		return positionMap;
	}

	@Override
	public Table postLoop(final SimulationState state) {
		if (attributes.isIgnoreEqualPositions()) {
			Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();
			table.clear();

			Set<Integer> pedIds = pedPosMap.keySet();

			for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
				int pedId = entry.getKey();
				VPoint position = entry.getValue();
				Row row = postUpdate(state, pedId, position);
				table.addRow(row);
			}
			return table;
		} else {
			return super.postLoop(state);
		}
	}

	@Override
	public PedestrianPositionProcessor clone() {
		return new PedestrianPositionProcessor(attributes);
	}

	@Override
	public String getFileExtension() {
		return IOUtils.TRAJECTORY_FILE_EXTENSION;
	}

	@Override
	public boolean isRequiredForVisualisation() {
		return true;
	}

	public static Pedestrian rowToPedestrian(final Row row, final AttributesAgent attributesPedestrian)
			throws NumberFormatException, NullPointerException {

		Pedestrian pedestrian = new Pedestrian(
				new AttributesAgent(attributesPedestrian, Integer.parseInt(row.getEntry("id").toString())),
				new Random());
		pedestrian.setPosition(new VPoint(Double.parseDouble(row.getEntry("x").toString()),
				Double.parseDouble(row.getEntry("y").toString())));
		LinkedList<Integer> targets = new LinkedList<>();

		if (row.getColumnNames().contains("targetId")) {
			targets.addFirst(Integer.parseInt(row.getEntry("targetId").toString()));
		} else {
			targets.addFirst(-1);
		}
		pedestrian.setTargets(targets);
		return pedestrian;

	}

	public static Step rowToStep(final Row row) throws NumberFormatException, NullPointerException {
		return new Step(Integer.parseInt(row.getEntry("step").toString()),
				Double.parseDouble(row.getEntry("time").toString()));
	}
}
