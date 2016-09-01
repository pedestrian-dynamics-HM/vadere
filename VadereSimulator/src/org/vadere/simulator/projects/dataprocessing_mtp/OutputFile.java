package org.vadere.simulator.projects.dataprocessing_mtp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public abstract class OutputFile<K extends Comparable<K>> {
	private String[] keyHeaders;
	private String fileName;

	private List<Integer> processorIds;
	private List<Processor<K, ?>> processors;

    private static String SEPARATOR = " ";

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

	@SuppressWarnings("unchecked")
	public void init(final ProcessorManager manager) {
		processorIds.forEach(pid -> this.processors.add((Processor<K, ?>) manager.getProcessor(pid)));
	}

	public void write() {
	    try {
            File file = new File(this.fileName);

            if (!file.exists())
                file.createNewFile();

            try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
                // Print header
                out.println(StringUtils.substringBeforeLast(
                		(this.keyHeaders == null || this.keyHeaders.length == 0
							? ""
							: String.join(SEPARATOR.toString(), this.keyHeaders) + SEPARATOR)
						+ this.processors.stream().map(p -> String.join(SEPARATOR.toString(), p.getHeaders()) + SEPARATOR).reduce("", (s1, s2) -> s1 + s2), SEPARATOR.toString()));

                this.processors.stream().flatMap(p -> p.getKeys().stream()).distinct()
                        .forEach(key -> printRow(out, key));

                out.flush();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
	}

	private void printRow(final PrintWriter out, final K key) {
		out.println(StringUtils.substringBeforeLast(
				(this.toStrings(key).length == 0
					? ""
					: String.join(SEPARATOR, String.join(SEPARATOR, this.toStrings(key)) + SEPARATOR))
				+ processors.stream().map(p -> String.join(SEPARATOR, p.toStrings(key)) + SEPARATOR).reduce("", (s1, s2) -> s1 + s2), SEPARATOR));
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
