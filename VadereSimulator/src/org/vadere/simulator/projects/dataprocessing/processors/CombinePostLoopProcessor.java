package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.data.Row;
import org.vadere.util.data.Table;

/**
 * This processor makes it possible to combine different
 * {@link AbstractProcessor} processors that utilize the postLoop instead of
 * postUpdate functions to write data in the table. With this, it is possible to get a Table with
 * columns of
 * multiple {@link AbstractProcessor}. Useful to get a table with attributes
 * and exit times, for example (see {@link PedestrianAttributesProcessor} and
 * {@link PedestrianLastPositionProcessor}).
 * 
 * 
 */
public class CombinePostLoopProcessor extends AbstractProcessor {

	private List<AbstractProcessor> processorList;

	@Expose
	private Set<String> allSupportedColumnNames;

	@Expose
	private Table table;

	public CombinePostLoopProcessor(final List<AbstractProcessor> processorList) {
		super(new Table());
		this.processorList = processorList;
		this.allSupportedColumnNames = new HashSet<>();
		for (AbstractProcessor processor : processorList) {
			for (String columnName : processor.getAllColumnNames()) {
				this.allSupportedColumnNames.add(columnName);
			}
		}
		table = getTable();
		table.clear(getAllColumnNames());
	}

	@Override
	public Table preLoop(SimulationState state) {
		for (AbstractProcessor processor : processorList) {
			processor.preLoop(state);
		}
		return super.preLoop(state);
	}

	@Override
	public Table postUpdate(SimulationState state) {
		for (AbstractProcessor processor : processorList) {
			processor.postUpdate(state);
		}

		return super.postUpdate(state);
	}

	@Override
	public Table postLoop(final SimulationState state) {

		if (table.isEmpty()) {
			table.clear();

			Table currentTable = null;
			for (AbstractProcessor processor : processorList) {
				Table ltable = processor.postLoop(state);
				if (currentTable == null) {
					currentTable = ltable;
				} else {
					currentTable.merge(ltable);
				}
			}

			Iterator<Row> it = currentTable.iterator();
			for (; it.hasNext();) {
				table.addRow();
				Row row = it.next();
				table.addColumnEntries(row);
			}
		}

		return table;
	}

	@Override
	public String[] getAllColumnNames() {
		return allSupportedColumnNames.toArray(new String[] {});
	}

	public List<AbstractProcessor> getProcessorList() {
		return processorList;
	}
}
