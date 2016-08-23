package org.vadere.simulator.projects.dataprocessing_mtp;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class LogFile<K extends Comparable<K>> {
	private String keyHeader;
	private String fileName;

	private List<Integer> processorIds;
	private List<Processor<K, ?>> processors;

	LogFile() {
		this.processors = new ArrayList<>();
	}

	protected void setKeyHeader(final String keyHeader) {
		this.keyHeader = keyHeader;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public void setProcessorIds(final List<Integer> processorIds) {
		this.processorIds = processorIds;
		this.processors.clear();
	}

	public void init(final ProcessorManager manager) {
		processorIds.forEach(pid -> this.processors.add((Processor<K, ?>) manager.getProcessor(pid)));
	}

	public void write() {
	    try {
            File file = new File(this.fileName);

            if (!file.exists())
                file.createNewFile();

            try (PrintWriter out = new PrintWriter(new FileWriter(file), true)) {
                // Print header
                out.println((this.keyHeader
                        + " "
                        + this.processors.stream().map(p -> p.getHeader()).reduce("", (s1, s2) -> s1 + " " + s2).trim()).trim());

                this.processors.stream().flatMap(p -> p.getKeys().stream()).distinct().sorted()
                        .forEach(key -> printRow(out, key, this.processors));

                out.flush();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
	}

	private void printRow(PrintWriter out, final K key, final List<Processor<K, ?>> ps) {
		out.println((this.toString(key)
				+ " "
				+ ps.stream().map(p -> p.toString(key)).reduce("", (s1, s2) -> s1 + " " + s2).trim()).trim());
	}

	public String toString(K key) {
		return key.toString();
	}

	public String getFileName() {
		return fileName;
	}

	public List<Integer> getProcessorIds() {
		return processorIds;
	}

}
