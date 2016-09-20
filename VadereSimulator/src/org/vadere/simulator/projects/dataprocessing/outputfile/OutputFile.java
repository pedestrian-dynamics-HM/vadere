package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for all types of output files.
 *
 * This class knows all the data processors of which the data should be saved.
 * It writes the data with the specified <tt>separator</tt> sign into a file specified by <tt>filename</tt>.
 *
 * @param <K> key type
 *
 */

public abstract class OutputFile<K extends Comparable<K>> {
	private String[] keyHeaders;
	private String fileName;

	private List<Integer> processorIds;
	private List<DataProcessor<K, ?>> dataProcessors;

    private String separator;

	protected OutputFile(final String... keyHeaders) {
		this.keyHeaders = keyHeaders;
		this.dataProcessors = new ArrayList<>();
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public void setProcessorIds(final List<Integer> processorIds) {
		this.processorIds = processorIds;
		this.dataProcessors.clear();
	}

	public String getSeparator() {
		return this.separator;
	}

	public void setSeparator(final String separator) {
		this.separator = separator;
	}

	@SuppressWarnings("unchecked")
	public void init(final ProcessorManager manager) {
		processorIds.forEach(pid -> this.dataProcessors.add((DataProcessor<K, ?>) manager.getProcessor(pid)));
	}

	public void write() {
		try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
			printHeader(out);

			this.dataProcessors.stream().flatMap(p -> p.getKeys().stream())
					.distinct().sorted()
					.forEach(key -> printRow(out, key));

			out.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
	}

	private List<String> getFieldHeaders() {
		return composeLine(keyHeaders, p -> Arrays.stream(p.getHeaders()));
	}

	public void printHeader(PrintWriter out) {
		writeLine(out, getFieldHeaders());
	}

	public String getHeader() {
		return String.join(this.separator, getFieldHeaders());
	}

	private void printRow(final PrintWriter out, final K key) {
		@SuppressWarnings("unchecked")
		final List<String> fields = composeLine(toStrings(key), p -> Arrays.stream(p.toStrings(key)));
		writeLine(out, fields);
	}

	private List<String> composeLine(String[] keyFieldArray, @SuppressWarnings("rawtypes") Function<DataProcessor, Stream<String>> valueFields) {
		final List<String> fields = new LinkedList<>(Arrays.asList(keyFieldArray));

		final List<String> processorFields = dataProcessors.stream()
				.flatMap(valueFields)
				.collect(Collectors.toList());
		fields.addAll(processorFields);
		return fields;
	}
	
	private void writeLine(PrintWriter out, final List<String> fields) {
		out.println(String.join(this.separator, fields));
	}

	/** Return the column headers as string or the empty array. */
	public String[] toStrings(K key) {
		return new String[] { key.toString() };
	}

	public String getFileName() {
		return fileName;
	}

	public List<Integer> getProcessorIds() {
		return processorIds;
	}

	@Override
	public String toString() {
		return fileName;
	}
}
