package org.vadere.simulator.projects.dataprocessing.writer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.OutputStream;
import java.util.*;

import org.apache.log4j.Logger;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.processors.Processor;
import org.vadere.simulator.projects.io.JsonSerializerProcessor;
import org.vadere.state.attributes.processors.AttributesWriter;
import org.vadere.util.data.Table;
import org.vadere.util.io.IOUtils;

public class ProcessorWriter extends AbstractProcessorWriter {

	public final static String JSON_ATTRIBUTE_NAME = "processWriters";

	private AttributesWriter attributes;
	private String[] columnNames = null;
	private String formatString = "";
	private String columnString = "";

	public ProcessorWriter(final OutputStream out, final Processor processor, final AttributesWriter attributes) {
		super(processor, out);
		this.attributes = attributes;
	}

	public ProcessorWriter(final Processor processor, final AttributesWriter attributes) {
		super(processor);
		this.attributes = attributes;
	}

	public void setColumnFormat(final String formatString, final String... columnNames) {
		this.formatString = formatString;
		this.columnNames = columnNames;
	}

	public String getFormatString() {
		if (this.columnNames != null) {
			return formatString;
		} else {
			return Arrays.stream(getProcessor().getAllColumnNames()).reduce("", (s1, s2) -> s1 + "%s ").trim();
		}
	}

	public void setColumns(final String... columnNames) {
		formatString = "";
		for (int col = 0; col < columnNames.length; col++) {
			if (col < columnNames.length - 1) {
				this.formatString += "%s ";
			} else {
				this.formatString += "%s";
			}
		}
		this.columnNames = columnNames.clone();
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	@Override
	protected String[] getColumnNames(final Table table) {
		if (columnNames != null) {
			return columnNames.clone();
		}
		return super.getColumnNames(table);
	}

	@Override
	protected void writeContent(final Table table) {
		/*
		 * If there is no format string is set, print everything => use the super method.
		 * Else use the format string.
		 */
		if (formatString.length() <= 0) {
			super.writeContent(table);
		} else {
			ListIterator<Object[]> rows = table.listArrayIterator(getColumnNames(table));

			while (rows.hasNext()) {
				Object[] row = rows.next();
				getPrintWriter().printf(formatString + "\n", row);
			}
		}
	}

	@Override
	protected void writeHeading(final Table table) {
		if (formatString.length() <= 0) {
			super.writeHeading(table);
		} else {
			if (columnString.length() <= 0) {
				columnString = formatString.replaceAll("%.", "%s");
			}
			getPrintWriter().printf(columnString + "\n", (Object[]) columnNames);
		}
		getPrintWriter().flush();
		setHasHeadlinePrinted();
	}

	public void setAttributes(final AttributesWriter attributes) {
		if (attributes == null) {
			throw new IllegalArgumentException("attributes is null.");
		}
		this.attributes = attributes;
	}

	@Override
	public void preLoop(final SimulationState state) {
		/*
		 * if(columnNames == null) {
		 * String[] procColumnNames = processor.getAllColumnNames();
		 * if(procColumnNames != null) {
		 * setColumns(processor.getAllColumnNames());
		 * }
		 * }
		 */

		if (columnNames != null) {
			getProcessor().addColumnNames(columnNames);
		}

		super.preLoop(state);
	}

	@Override
	public void update(final SimulationState state) {
		if (attributes.getStartTime() <= state.getSimTimeInSec()
				&& attributes.getEndTime() >= state.getSimTimeInSec()) {
			super.update(state);
		}
	}



	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		ProcessorWriter tmp = (ProcessorWriter) obj;
		if (!formatString.equals(tmp.formatString)) {
			return false;
		}
		// check columNames
		if (this.columnNames != null) {
			if (this.columnNames.length != tmp.columnNames.length) {
				return false;
			}

			for (int col = 0; col < this.columnNames.length; col++) {
				if (!this.columnNames[col].equals(tmp.columnNames[col])) {
					return false;
				}
			}
		} else {
			if (tmp.columnNames != null) {
				return false;
			}
		}

		return getProcessor().equals(tmp.getProcessor());
	}

	@Override
	public int hashCode() {
		int result = attributes.hashCode();
		result = 31 * result + (columnNames != null ? Arrays.hashCode(columnNames) : 0);
		result = 47 * result + formatString.hashCode();
		result = 31 * result + columnString.hashCode();
		return result;
	}

	// JSON Stuff!
	@Override
	@Deprecated
	public JsonElement toJson() {
		Map<String, Object> store = new HashMap<>();
		store.put("columnNames", this.columnNames);
		store.put("formatString", this.formatString);
		store.put("processor", JsonSerializerProcessor.toJsonElement(getProcessor()));
		store.put("attributes", attributes);
		return IOUtils.getGson().toJsonTree(store);
	}

	@Deprecated
	public static JsonElement toJson(final List<ProcessorWriter> writers) {
		JsonArray jsonArray = new JsonArray();
		for (ProcessorWriter writer : writers) {
			jsonArray.add(writer.toJson());
		}
		return jsonArray;
	}

	@SuppressWarnings("unchecked")
	@Deprecated
	public static ProcessorWriter fromJson(final JsonElement jsonEl) {
		Map<String, Object> store = new HashMap<>();
		store = new Gson().fromJson(jsonEl.toString(), Map.class);
		AttributesWriter attributes =
				IOUtils.getGson().fromJson(IOUtils.toJson(store.get("attributes")), AttributesWriter.class);
		String columnFormat = IOUtils.getGson().fromJson(IOUtils.toJson(store.get("formatString")), String.class);
		List<String> columnNames = IOUtils.getGson().fromJson(IOUtils.toJson(store.get("columnNames")), List.class);
		Processor processor = JsonSerializerProcessor.toProcessorFromJson(IOUtils.toJson(store.get("processor")));
		ProcessorWriter writer = new ProcessorWriter(processor, attributes);
		writer.formatString = columnFormat;

		if (columnNames == null) {
			writer.columnNames = null;
		} else {
			writer.columnNames = columnNames.toArray(new String[] {});
		}
		return writer;
	}

	@Deprecated
	public static List<ProcessorWriter> fromJsonList(final String json) {
		List<ProcessorWriter> writerList = new ArrayList<>();
		JsonElement jsonElement = new Gson().fromJson(json, JsonElement.class);

		if (jsonElement instanceof JsonArray) {
			JsonArray array = (JsonArray) jsonElement;
			for (int index = 0; index < array.size(); index++) {
				try {
					writerList.add(ProcessorWriter.fromJson(array.get(index)));
				} catch (Exception e) {
					Logger.getLogger(ProcessorWriter.class).error(e);
				}
			}
		}

		return writerList;
	}
}
