package org.vadere.simulator.projects.dataprocessing_mtp;

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

public abstract class OutputFile<K extends Comparable<K>> {
	private String[] keyHeaders;
	private String fileName;

	private List<Integer> processorIds;
	private List<Processor<K, ?>> processors;

    private String separator;

	OutputFile(final String... keyHeaders) {
		this.keyHeaders = keyHeaders;
		this.processors = new ArrayList<>();
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public void setProcessorIds(final List<Integer> processorIds) {
		this.processorIds = processorIds;
		this.processors.clear();
	}

	public String getSeparator() {
		return this.separator;
	}

	public void setSeparator(final String separator) {
		this.separator = separator;
	}

	@SuppressWarnings("unchecked")
	public void init(final ProcessorManager manager) {
		processorIds.forEach(pid -> this.processors.add((Processor<K, ?>) manager.getProcessor(pid)));
	}

	public void write() {
		try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
			printHeader(out);

			this.processors.stream().flatMap(p -> p.getKeys().stream())
					.distinct().sorted()
					.forEach(key -> printRow(out, key));

			out.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
	}

	private void printHeader(PrintWriter out) {
		// Print headers only if the line is not empty, i.e. the keyHeaders and/or processor headers are given
		if (keyHeaders.length + this.processors.stream().map(p -> p.getHeaders().length).reduce(0, (x,y) -> x+y) > 0) {
			final List<String> fieldHeaders = composeLine(keyHeaders, p -> Arrays.stream(p.getHeaders()));
			writeLine(out, fieldHeaders);
		}
	}

	private void printRow(final PrintWriter out, final K key) {
		@SuppressWarnings("unchecked")
		final List<String> fields = composeLine(toStrings(key), p -> Arrays.stream(p.toStrings(key)));
		writeLine(out, fields);
	}

	private List<String> composeLine(String[] keyFieldArray, @SuppressWarnings("rawtypes") Function<Processor, Stream<String>> valueFields) {
		final List<String> fields = new LinkedList<>(Arrays.asList(keyFieldArray));

		final List<String> processorFields = processors.stream()
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
}
