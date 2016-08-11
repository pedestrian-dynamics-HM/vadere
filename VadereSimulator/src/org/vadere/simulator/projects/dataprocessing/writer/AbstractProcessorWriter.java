package org.vadere.simulator.projects.dataprocessing.writer;

import com.google.gson.JsonElement;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ListIterator;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.processors.Processor;
import org.vadere.util.data.Table;

public abstract class AbstractProcessorWriter implements Writer {

	private PrintWriter out = null;
	private Processor processor;
	private boolean hasHeadlinePrinted;
	private boolean writeHeader;

	public AbstractProcessorWriter(final Processor processor) {
		this.processor = processor;
		this.hasHeadlinePrinted = false;
		this.writeHeader = true;
	}

	public AbstractProcessorWriter(final Processor processor, final OutputStream out) {
		this(processor);
		this.out = new PrintWriter(out, true);
	}

	@Override
	public void preLoop(final SimulationState state) {
		hasHeadlinePrinted = false;
		Table table = processor.preLoop(state);
		write(table);
	}

	@Override
	public void postLoop(final SimulationState state) {
		Table table = processor.postLoop(state);
		write(table);
		close();
	}

	@Override
	public void update(final SimulationState state) {
		Table table = processor.postUpdate(state);
		write(table);
	}

	public Processor getProcessor() {
		return processor;
	}

	public void setProcessor(final Processor processor) {
		if (processor == null) {
			throw new IllegalArgumentException("processor is null.");
		}
		this.processor = processor;
	}

	private void write(final Table table) {
		if (isTableValid(table)) {
			if (!hasHeadlinePrinted() && writeHeader) {
				writeHeading(table);
			}
			writeContent(table);
		}
	}

	/**
	 * Closes the output stream. Needed for appendMode.
	 */
	public void close() {
		this.out.close();
	}

	protected String[] getColumnNames(final Table table) {
		return table.getColumnNames().clone();
	}

	protected boolean hasHeadlinePrinted() {
		return hasHeadlinePrinted;
	}

	protected boolean isTableValid(final Table table) {
		return table != null && !table.isEmpty();
	}

	protected void writeContent(final Table table) {
		ListIterator<Object[]> iterator = table.listArrayIterator(getColumnNames(table));

		while (iterator.hasNext()) {
			Object[] obj = iterator.next();
			for (int col = 0; col < obj.length; col++) {
				if (col < obj.length - 1) {
					out.print(obj[col].toString() + " ");
				} else {
					out.print(obj[col].toString() + "\n");
				}
			}
		}
		out.flush();
	}

	protected void writeHeading(final Table table) {
		String[] columnNames = getColumnNames(table);
		for (int col = 0; col < columnNames.length; col++) {
			if (col < columnNames.length - 1) {
				out.print(columnNames[col] + " ");
			} else {
				out.print(columnNames[col] + "\n");
			}
		}
		out.flush();
		setHasHeadlinePrinted();
	}

	protected void setHasHeadlinePrinted() {
		hasHeadlinePrinted = true;
	}

	protected PrintWriter getPrintWriter() {
		return out;
	}

	public void setWriteHeader(final boolean writeHeader) {
		this.writeHeader = writeHeader;
	}

	public void setOutputStream(final OutputStream out) {
		this.out = new PrintWriter(out, true);
	}

	public boolean hasOutputStream() {
		return this.out != null;
	}

	@Override
	public String toString() {
		return processor.getName();
	}

	public abstract JsonElement toJson();
}
