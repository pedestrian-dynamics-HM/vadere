

package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.dataprocessing.datakey.DataKey;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriter;
import org.vadere.simulator.projects.dataprocessing.writer.VadereWriterFactory;
import org.vadere.util.logging.Logger;

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

	private static Logger logger = Logger.getLogger(OutputFile.class);

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
	private boolean isWriteMetaData;

	private String separator;

	// Check also the PostVis where there is a dependency
	public final static String headerProcSep = "-";
	public final static String headerNameAdd = headerProcSep + "PID?"; // the # is replaced with the processor id

	private VadereWriterFactory writerFactory;
	private VadereWriter writer;

	protected OutputFile(final String... dataIndices) {
		this.dataIndices = dataIndices;
		this.isAddedProcessors = false;  // init method has to be called
        this.isWriteMetaData = false;
		this.dataProcessors = new ArrayList<>();
		this.writerFactory = VadereWriterFactory.getFileWriterFactory();
	}

	public void setAbsoluteFileName(final String fileName) {
	    this.absoluteFileName = fileName;
	}

	public void setWriteMetaData(boolean isWriteMetaData){
	    this.isWriteMetaData = isWriteMetaData;
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
			logger.info("Absolute file name" + absoluteFileName);
			try (VadereWriter out = writerFactory.create(absoluteFileName)) {

			    this.writer = out;

				if(this.isWriteMetaData){
					printMetaData();
				}

				printHeader();

				this.dataProcessors.stream().flatMap(p -> p.getKeys().stream())
						.distinct().sorted()
						.forEach(key -> printRow(key));

				out.flush();
			} catch (IOException e) {
				logger.error(e.getMessage());
				throw new UncheckedIOException(e);
			}
		}
	}

	public boolean isEmpty() {
		return this.dataProcessors.isEmpty();
	}

	private void printMetaData(){
		// use '#' symbol for comment -- the Java-style comment '//' is not appropriate because it requires two
		// characters
		String md = "#IDXCOL=" + dataIndices.length +
				",DATACOL="+(getEntireHeader().size()-dataIndices.length)+","+
				"SEP=\'"+ DataProcessingJsonManager.DEFAULT_SEPARATOR +"\'";

		//Make a list with one element to reuse 'writeLine' function
		List<String> line = new LinkedList<>();
		line.add(md);

		writeLine(this.writer, line);
	}

	void printHeader() {
		writeLine(this.writer, this.getEntireHeader());
	}

	private void printRow(final K key) {
		// Info: 'key' are the indices values (such as timeStep=3), can be more than one
		@SuppressWarnings("unchecked")
		final List<String> fields = composeLine(toStrings(key), p ->
				Arrays.stream(p.toStrings(key)));
		writeLine(this.writer, fields);
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
		return String.join(DataProcessingJsonManager.DEFAULT_SEPARATOR, this.getEntireHeader());
	}

	public String getIndicesLine() {
		return String.join(DataProcessingJsonManager.DEFAULT_SEPARATOR, this.getIndices());
	}

	public static String addHeaderProcInfo(String columnName, int procId){
		return columnName + headerNameAdd.replace("?", "" + procId);
	}

	private List<String> uniqueHeaderNames(){
		// This function adds to every header "headerNameAdd", for ALL headers EVERY time
		// (cmp. headersWithNameMangling)
		LinkedList<String> headers = new LinkedList<>();

		for (DataProcessor dataProcessor: dataProcessors) {
			List<String> allProcHeaders = Arrays.asList(dataProcessor.getHeaders());

			for (String singleHeader: allProcHeaders) {
				// add the processor id to make header unique
				String adaptedColumnName = OutputFile.addHeaderProcInfo(singleHeader, dataProcessor.getId());
				headers.addLast(adaptedColumnName);
			}
		}

		return headers;
	}

	private List<String> headersWithNameMangling(){
		// This function adds to every header "headerNameAdd", ONLY if there is a name conflict detected
		// (cmp. uniqueHeaderNames)

		LinkedList<String> headers = new LinkedList<>();
		boolean isNameMangleDetected = false; // assume there is no nameing conflict

		mainloop:
		for (DataProcessor l: dataProcessors) {
			List<String> list = Arrays.asList(l.getHeaders());

			for(String el: list) {
				if(headers.contains(el)){
					isNameMangleDetected = true;  // conflict found: stop collecting and name make every header unique
					break mainloop;
				}else{
					headers.addLast(el);
				}
			}
		}

		if(isNameMangleDetected){
			headers = (LinkedList<String>) uniqueHeaderNames();
		}

		return headers;
	}

	private List<String> composeHeaderLine(){
		final List<String> allHeaders = new LinkedList<>(Arrays.asList(dataIndices));
		List<String> procHeaders = this.uniqueHeaderNames();

		allHeaders.addAll(procHeaders);

		return allHeaders;
	}

	private List<String> composeLine(String[] keyFieldArray,
									 @SuppressWarnings("rawtypes") Function<DataProcessor, Stream<String>> valueFields){
		final List<String> fields = new  LinkedList<>();

		addkeysToLine(fields, keyFieldArray);
		addProcessorToLine(fields, valueFields);

		return fields;
	}

	List<String> addkeysToLine(final List<String> fields, String[] keyFieldArray){
		fields.addAll(Arrays.asList(keyFieldArray));
		return fields;
	}

	List<String> addProcessorToLine(final List<String> fields, Function<DataProcessor, Stream<String>> valueFields){
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