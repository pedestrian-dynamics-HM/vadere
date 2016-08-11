package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.data.Row;
import org.vadere.util.data.Table;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * This processor makes it possible to combine different {@link ForEachPedestrianPositionProcessor}
 * processors.
 * So it's possible to get a Table with columns of multiple
 * {@link ForEachPedestrianPositionProcessor}.
 * Useful for get a table with velocity and density or different densities in one table.
 * 
 *
 */
public class CombineProcessor extends AbstractProcessor {

	private List<ForEachPedestrianPositionProcessor> processorList;

	@Expose
	private Set<String> allSupportedColumnNames;

	@Expose
	private Table table;

	@Expose
	private int lastStep;

	public CombineProcessor(final List<ForEachPedestrianPositionProcessor> processorList) {
		super(new Table());
		this.processorList = processorList;
		this.allSupportedColumnNames = new HashSet<>();
		for (ForEachPedestrianPositionProcessor processor : processorList) {
			for (String columnName : processor.getAllColumnNames()) {
				this.allSupportedColumnNames.add(columnName);
			}
		}
		table = getTable();
		table.clear(getAllColumnNames());
		lastStep = 0;
	}

	@Override
	public Table preLoop(SimulationState state) {
		for (ForEachPedestrianPositionProcessor processor : processorList) {
			processor.preLoop(state);
		}

		return super.preLoop(state);
	}

	@Override
	public Table postUpdate(final SimulationState state) {

		if (table.isEmpty() || state.getStep() != lastStep) {
			Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();
			table.clear();

			// 1. for each pedestrian position calculate column entries and build a row for each
			// pedestrian
			for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
				Set<String> insertedColumn = new HashSet<>();
				table.addRow();
				for (ForEachPedestrianPositionProcessor processor : processorList) {
					Row row = processor.postUpdate(state, entry.getKey(), entry.getValue());
					for (String colName : row.getColumnNames()) {
						if (!insertedColumn.contains(colName)) {
							table.addColumnEntry(colName, row.getEntry(colName));
							insertedColumn.add(colName);
						}

					}

				}
			}
		}

		lastStep = state.getStep();

		return table;
	}

	@Override
	public String[] getAllColumnNames() {
		return allSupportedColumnNames.toArray(new String[] {});
	}

	public List<ForEachPedestrianPositionProcessor> getProcessorList() {
		return processorList;
	}
}
