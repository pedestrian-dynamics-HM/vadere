package org.vadere.util.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Row {
	private final Map<String, Object> row;

	public Row() {
		row = new HashMap<>();
	}

	public Row(String... columnNames) {
		this();
		for (String colName : columnNames) {
			row.put(colName, "");
		}
	}

	public void setEntry(final String columName, final Object value) {
		row.put(columName, value);
	}

	public Object getEntry(final String columnName) {
		return row.get(columnName);
	}

	public Set<String> getColumnNames() {
		return row.keySet();
	}

	public void merge(Row row) {
		for (String name : row.getColumnNames()) {
			if (!this.row.containsKey(name)) {
				this.row.put(name, row.getEntry(name));
			}
		}
	}

	public Row combine(final Row row) {
		Row result = new Row();
		result.merge(this);
		result.merge(row);
		return result;
	}
}
