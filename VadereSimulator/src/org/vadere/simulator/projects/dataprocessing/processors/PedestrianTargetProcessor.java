package org.vadere.simulator.projects.dataprocessing.processors;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.processors.AttributesPedestrianTargetProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import com.google.gson.annotations.Expose;

/**
 * Adds information about the targets of the pedestrian of the simulation to the table.
 * 
 * <p>
 * <b>Added column names</b>: id {@link Integer}, designatedTargetID {@link Double},
 * reachedLastTarget {@link Double}
 * </p>
 * 
 *
 */
public class PedestrianTargetProcessor extends AbstractProcessor {

	private AttributesPedestrianTargetProcessor attributes;
	private boolean firstRun;
	private Collection<Pedestrian> pedestrians;
	private List<Pedestrian> originalPedestrians;

	@Expose
	private Table table;

	@Expose
	private Map<Integer, Integer> targetIds;

	@Expose
	private Map<Integer, Integer> sourceIds;

	@Expose
	private Map<Integer, Boolean> reachedRightTarget;

	public PedestrianTargetProcessor(final AttributesPedestrianTargetProcessor attributes) {
		super(new Table("id", "designatedTargetID", "reachedLastTarget"));
		this.table = getTable();
		this.attributes = attributes;
		targetIds = new HashMap<>();
		sourceIds = new HashMap<>();
		reachedRightTarget = new HashMap<>();
		firstRun = true;
	}

	public PedestrianTargetProcessor() {
		this(new AttributesPedestrianTargetProcessor());
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		pedestrians = state.getTopography().getElements(Pedestrian.class);
		if (firstRun) {
			firstRun = false;
			originalPedestrians = new ArrayList<Pedestrian>();
			for (Pedestrian p : pedestrians) {
				reachedRightTarget.put(p.getId(), false);
				originalPedestrians.add(p);
			}
		}
		for (Pedestrian p : pedestrians) {
			int id = p.getId();
			VPoint pos = p.getPosition();
			LinkedList<Integer> pedestrianTargets = p.getTargets();
			int lastTargetID = pedestrianTargets.getLast();
			List<Target> targetList = state.getTopography().getTargets();
			Target lastTarget = null;
			for (Target t : targetList) {
				if (t.getId() == lastTargetID) {
					lastTarget = t;
				}
			}
			VShape shape = lastTarget.getShape();
			Rectangle bounds = shape.getBounds();
			if (shape.contains(pos)) {
				reachedRightTarget.put(id, true);
			}
		}

		return super.postUpdate(state);
	}


	@Override
	public Table postLoop(final SimulationState state) {

		if (table.isEmpty()) {
			double numberOfPedestrians = originalPedestrians.size();
			double correctResults = 0;
			for (Pedestrian p : originalPedestrians) {
				int id = p.getId();
				// table.addRow();
				// table.addColumnEntry("id", id);
				// table.addColumnEntry("designatedTargetID", p.getTargets().getLast());
				// if(reachedRightTarget.get(id)){
				// correctResults ++;
				// table.addColumnEntry("reachedLastTarget", "true");
				// }else{
				// table.addColumnEntry("reachedLastTarget", "false");
				// }
			}
			double rate = correctResults / numberOfPedestrians;
			table.addRow();
			table.addColumnEntry("id", "Rate of Pedestrians which reached their target:");
			table.addColumnEntry("designatedTargetID", rate);
			table.addColumnEntry("reachedLastTarget", "");
			table.addRow();
			if (attributes.getMinEvacRate() <= rate) {
				table.addColumnEntry("id", "SUCCESS");
			} else {
				table.addColumnEntry("id", "FAILED");
			}
			table.addColumnEntry("designatedTargetID", "");
			table.addColumnEntry("reachedLastTarget", "");

		}
		return table;
	}

	@Override
	public PedestrianTargetProcessor clone() {
		return new PedestrianTargetProcessor(this.attributes);
	}
}
