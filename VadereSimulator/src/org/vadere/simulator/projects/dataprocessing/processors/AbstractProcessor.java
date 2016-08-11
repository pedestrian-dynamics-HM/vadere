package org.vadere.simulator.projects.dataprocessing.processors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.Attributes;
import org.vadere.util.data.Table;
import org.vadere.util.io.IOUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Share common code.
 * 
 *
 */
public abstract class AbstractProcessor implements Processor {

	@Expose
	@JsonIgnore
	private static Logger logger = LogManager.getLogger(AbstractProcessor.class);

	private Set<String> columnNames;

	private String clazz;

	@Expose
	@JsonIgnore
	private Table table;

	@Expose
	@JsonIgnore
	protected Set<String> supportedColumnNames;

	protected AbstractProcessor() {
		this(new Table(), null);
	}

	protected AbstractProcessor(final Table table) {
		this(table, null);
	}

	protected AbstractProcessor(final Table table, final Attributes attributes) {
		columnNames = new HashSet<>();
		supportedColumnNames = new HashSet<>();
		this.table = table;
		this.clazz = this.getClass().getSimpleName().toString();

		for (String colName : table.getColumnNames()) {
			supportedColumnNames.add(colName);
		}
	}

	protected boolean isColumnVisible(final String columnName) {
		return columnNames.contains(columnName);
	}

	protected boolean isColumnSupported(final String columnName) {
		return table.containsColumn(columnName);
	}

	protected Table getTable() {
		return table;
	}

	protected boolean addColumnName(final String columnName) {
		boolean added = false;
		if (isColumnSupported(columnName)) {
			this.columnNames.add(columnName);
			added = true;
		}
		return added;
	}

	@Override
	public String[] getAllColumnNames() {
		return table.getColumnNames();
	}

	@Override
	public Attributes getAttributes() {
		return null;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void addColumnNames(final String... columnNames) {
		for (String columnName : columnNames) {
			if (!addColumnName(columnName)) {
				// logger.warn(columnName + " is an unsupported columnName.");
			}
		}
	}

	@Override
	public Table postLoop(final SimulationState state) {
		return new Table(new String[] {});
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		return new Table(new String[] {});
	}

	@Override
	public Table preLoop(final SimulationState state) {
		table.clear();
		return table;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		AbstractProcessor tmp = (AbstractProcessor) obj;

		return clazz.equals(tmp.clazz) && columnNames.equals(tmp.columnNames);
	}

	@Override
	public int hashCode() {
		return 31 * (clazz.hashCode() + columnNames.hashCode());
	}

	public String getFileExtension() {
		return IOUtils.OUTPUT_FILE_EXTENSION;
	}

	@Override
	public boolean isRequiredForVisualisation() {
		return false;
	}
}
