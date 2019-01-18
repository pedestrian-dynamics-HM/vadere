package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.datakey.DataKey;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriter;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import sun.awt.image.ImageWatched;

import javax.sound.midi.SysexMessage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for all types of output files.
 *
 * This class knows all the data processors of which the data should be saved. It writes the data
 * with the specified <tt>separator</tt> sign into a file specified by <tt>filename</tt>.
 *
 * @param <K> key type
 * @author Mario Teixeira Parente
 */

public abstract class OutputFile<K extends DataKey<K>> {


	//Note: Only header information from data keys are written in this, therefore, these are the indices
	// Each processor attached to this processor itself attaches also headers, but they are not listed in this
	// attribute.
	private String[] dataIndices;

	/**
	 * The file name without the path to the file
	 */
	private String fileName;

	/**
	 * Temporary absolute file name, this should not be serialized
	 */
	private volatile String absoluteFileName;

	private List<Integer> processorIds;
	private List<DataProcessor<K, ?>> dataProcessors;
	private boolean isAddedProcessors;

	private String separator;
	private final String nameConflictAdd = "-Proc?"; // the # is replaced with the processor id
	private VadereWriterFactory writerFactory;
	private VadereWriter writer;

	protected OutputFile(final String... dataIndices) {
		this.dataIndices = dataIndices;
		this.isAddedProcessors = false;  // init method has to be called
		this.dataProcessors = new ArrayList<>();
		this.writerFactory = VadereWriterFactory.getFileWriterFactory();
	}

	public void setAbsoluteFileName(final String fileName) {
		this.absoluteFileName = fileName;
	}

	public void setRelativeFileName(final String fileName) {
		this.fileName = fileName;

	}

	public String getSeparator() {
		return this.separator;
	}

	public void setSeparator(final String separator) {
		this.separator = separator;
	}

	@SuppressWarnings("unchecked")
	public void init(final Map<Integer, DataProcessor<?, ?>> processorMap) {
		this.dataProcessors.clear();
		processorIds.forEach(pid -> {
			Optional.ofNullable(processorMap.get(pid))
					.ifPresent(p -> dataProcessors.add((DataProcessor<K, ?>) p));
		});

		this.isAddedProcessors = true;
	}

	public void write() {
		if (!isEmpty()) {
			try (VadereWriter out = writerFactory.create(absoluteFileName)) {
                writer = out;
				printHeader(out);

				this.dataProcessors.stream().flatMap(p -> p.getKeys().stream())
						.distinct().sorted()
						.forEach(key -> printRow(out, key));

				out.flush();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	public boolean isEmpty() {
		return this.dataProcessors.isEmpty();
	}

	private void printHeader(VadereWriter out) {
		writeLine(out, this.getEntireHeader());
	}

	private List<String> getIndices(){
		return new LinkedList<>(Arrays.asList(dataIndices));
	}

	public List<String> getEntireHeader() {

		if(! isAddedProcessors){
			throw new RuntimeException("Asking for headers, but processors were not " +
					"initialized yet.");
		}

		//"getHeaders" is called in class DataProcessor.java, the
		return composeHeaderLine();
	}

	public String getHeaderLine() {
		return String.join(this.separator, this.getEntireHeader());
	}

	public String getIndicesLine() {
		return String.join(this.separator, this.getIndices());
	}

	private void printRow(final VadereWriter out, final K key) {
		// Info: 'key' are the indices values (such as timeStep=3), can be more than one
		@SuppressWarnings("unchecked")
		final List<String> fields = composeLine(toStrings(key), p -> Arrays.stream(p.toStrings(key)));
		writeLine(out, fields);
	}

	private List<String> headersWithNameMangling(){
		LinkedList<String> headers = new LinkedList<>();
		boolean isNameMangle = false; // assume there is no nameing conflict

		mainloop:
		for (DataProcessor l: dataProcessors) {
			List<String> list = Arrays.asList(l.getHeaders());

			for(String el: list) {
				if(headers.contains(el)){
					isNameMangle = true;  // conflict found: stop collecting headers
					break mainloop;
				}else{
					headers.addLast(el);
				}
			}
		}

		if(isNameMangle){
 			headers.clear();  //start from new...
			for (DataProcessor l: dataProcessors) {
				List<String> list = Arrays.asList(l.getHeaders());

				for (String h: list) {
					// ... but now add the processor id
					headers.addLast(h +
							this.nameConflictAdd.replace('?', (char) (l.getId()+'0')));
				}
			}
		}
		return headers;
	}

	private List<String> composeHeaderLine(){
		final List<String> allHeaders = new LinkedList<>(Arrays.asList(dataIndices));
		List<String> procHeaders = this.headersWithNameMangling();

		allHeaders.addAll(procHeaders);

		return allHeaders;
	}

	private List<String> composeLine(String[] keyFieldArray, @SuppressWarnings("rawtypes") Function<DataProcessor, Stream<String>> valueFields) {
		final List<String> fields = new LinkedList<>(Arrays.asList(keyFieldArray));

		final List<String> processorFields = dataProcessors.stream()
				.flatMap(valueFields)
				.collect(Collectors.toList());

		fields.addAll(processorFields);

		return fields;
	}

	private void writeLine(VadereWriter out, final List<String> fields) {
		out.println(String.join(this.separator, fields));
	}

	/**
	 * Return the column indices as string or the empty array.
	 */
	public String[] toStrings(K key) {
		return new String[]{key.toString()};
	}

	public String getFileName() {
		return fileName;
	}

	public List<Integer> getProcessorIds() {
		return processorIds;
	}

	public void setProcessorIds(final List<Integer> processorIds) {
		this.processorIds = processorIds;
		this.dataProcessors.clear();
	}

	@Override
	public String toString() {
		return new File(fileName).getName();
	}

	public void setVadereWriterFactory(VadereWriterFactory writerFactory) {
		this.writerFactory = writerFactory;
	}
}
