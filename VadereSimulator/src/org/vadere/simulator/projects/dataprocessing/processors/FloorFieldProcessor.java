package org.vadere.simulator.projects.dataprocessing.processors;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.OutputGenerator;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.potential.fields.PotentialFieldTargetGrid;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.processors.AttributesFloorFieldProcessor;
import org.vadere.util.data.Table;

public class FloorFieldProcessor extends AbstractProcessor {

	private static Logger logger = LogManager.getLogger(FloorFieldProcessor.class);
	private AttributesFloorFieldProcessor attributes;
	@JsonIgnore
	private Table lastTable;

	public FloorFieldProcessor() {
		this(new AttributesFloorFieldProcessor());
	}

	public FloorFieldProcessor(final AttributesFloorFieldProcessor attributes) {
		super(new Table(), attributes);
		this.attributes = attributes;
		this.lastTable = null;
	}

	@Override
	public String[] getAllColumnNames() {
		return null;
	}

	@Override
	public Table postUpdate(final SimulationState state) {

		Optional<OutputGenerator> optGenerator = state.getOutputGenerator(PotentialFieldTargetGrid.class);
		Table table = null;

		if (optGenerator.isPresent()) {
			OutputGenerator generator = optGenerator.get();
			Map<String, Table> tables = generator.getOutputTables();
			String tableName = String.valueOf(attributes.getTargetId());

			if (tables == null || tables.get(tableName) == null) {
				table = new Table();
				logger.warn("missing OutputGenerator for " + AttributesFloorField.class + " for target "
						+ attributes.getTargetId());
			} else if (!tables.get(tableName).equals(lastTable)) {
				table = new Table(new String[] {"step", "time"},
						new Object[] {state.getStep(), state.getSimTimeInSec()}, tables.get(tableName).size());
				table.merge(tables.get(tableName));
			}

			if (tables != null) {
				lastTable = tables.get(tableName);
			}
		} else {
			table = new Table();
			logger.warn("missing OutputGenerator for " + AttributesFloorField.class + " for any target");
		}

		return table;
	}

	@Override
	public String getName() {
		return FloorFieldProcessor.class.getSimpleName();
	}

	@Override
	public Object clone() {
		return new FloorFieldProcessor(attributes);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;

		FloorFieldProcessor that = (FloorFieldProcessor) o;

		if (!attributes.equals(that.attributes))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + attributes.hashCode();
		return result;
	}
}
