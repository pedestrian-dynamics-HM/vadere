package org.vadere.simulator.projects.dataprocessing.processors;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.data.Table;

import com.google.gson.annotations.Expose;

/**
 * Adds the mean evacuation time of all pedestrians to the Table.
 * 
 * <p>
 * <b>Added column names</b>: meanEvacTime {@link Double}
 * </p>
 *
 *
 */
public class MeanEvacuationTimeProcessor extends AbstractProcessor {

	@Expose
	private Table table;

	private PedestrianLastPositionProcessor lastPosProcessor;

	public MeanEvacuationTimeProcessor() {
		this(new PedestrianLastPositionProcessor());
	}

	public MeanEvacuationTimeProcessor(final PedestrianLastPositionProcessor lastPosProcessor) {
		super(new Table("meanEvacTime"));
		this.lastPosProcessor = lastPosProcessor;
		table = getTable();
	}

	@Override
	public Table postLoop(final SimulationState state) {
		lastPosProcessor.postLoop(state);
		table.addRow();
		table.addColumnEntry("meanEvacTime", lastPosProcessor.getMeanEvactuationTime());
		return table;
	}

	@Override
	public Table postUpdate(SimulationState state) {
		lastPosProcessor.postUpdate(state);
		return super.postUpdate(state);
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	@Override
	public MeanEvacuationTimeProcessor clone() {
		return new MeanEvacuationTimeProcessor(lastPosProcessor.clone());
	}

	@Override
	public boolean equals(final Object obj) {
		if (super.equals(obj)) {
			MeanEvacuationTimeProcessor tmp = (MeanEvacuationTimeProcessor) obj;
			if (lastPosProcessor == null) {
				return tmp.lastPosProcessor == null;
			} else {
				return lastPosProcessor.equals(tmp.lastPosProcessor);
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + lastPosProcessor.hashCode();
		return result;
	}
}
