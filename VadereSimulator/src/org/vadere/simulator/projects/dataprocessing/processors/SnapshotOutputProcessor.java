package org.vadere.simulator.projects.dataprocessing.processors;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.util.data.Table;
import org.vadere.util.io.IOUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.annotations.Expose;

public class SnapshotOutputProcessor extends AbstractProcessor {

	@Expose
	private Table table;

	public SnapshotOutputProcessor() {
		super(new Table("snapshot"));

		table = getTable();
	}

	@Override
	public Table preLoop(final SimulationState state) {
		table.addRow();
		try {
			table.addColumnEntry("snapshot", JsonConverter.serializeSimulationStateSnapshot(state.getScenarioStore(),
					state.getProcessorWriter(), true));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		Table tmpTable = table;
		table = new Table();
		return tmpTable;
	}

	@Override
	public String getFileExtension() {
		return IOUtils.SCENARIO_FILE_EXTENSION;
	}

	@Override
	public boolean isRequiredForVisualisation() {
		return true;
	}
}
