package org.vadere.simulator.projects.dataprocessing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.dataprocessing.outputfile.NoDataKeyOutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFileFactory;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessorFactory;
import org.vadere.simulator.projects.dataprocessing.store.DataProcessorStore;
import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.StateJsonConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mario Teixeira Parente
 */

public class DataProcessingJsonManager {

	public static final String DATAPROCCESSING_KEY = "processWriters";

	public static final String TRAJECTORIES_FILENAME = "postvis.traj";

	public static final String FILES_KEY = "files";
	public static final String PROCESSORS_KEY = "processors";
	public static final String ATTRIBUTES_KEY = "attributes";
	public static final String DEFAULT_SEPARATOR = " ";
	public static final String DEFAULT_OUTPUTFILE_TYPE = NoDataKeyOutputFile.class.getName();
	public static final String DEFAULT_NAME = "outputFile";
	private static final String TYPE_KEY = "type";
	private static final String FILENAME_KEY = "filename";
	private static final String FILE_PROCESSORS_KEY = "processors";
	private static final String SEPARATOR_KEY = "separator";
	private static final String PROCESSORID_KEY = "id";
	private static final String ATTRIBUTESTYPE_KEY = "attributesType";
	private static final String TIMESTAMP_KEY = "isTimestamped";
	private static final String WRITEMETA_KEY = "isWriteMetaData";
	public static ObjectWriter writer;
	private static ObjectMapper mapper;

	static {
		mapper = StateJsonConverter.getMapper();
		writer = mapper.writerWithDefaultPrettyPrinter();
	}

	private final OutputFileFactory outputFileFactory;
	private final DataProcessorFactory processorFactory;
	private List<OutputFile<?>> outputFiles;
	private List<DataProcessor<?, ?>> dataProcessors;
	private boolean isTimestamped;
	private boolean isWriteMetaData;

	public DataProcessingJsonManager() {
		this.outputFiles = new ArrayList<>();
		this.dataProcessors = new ArrayList<>();
		this.isTimestamped = true;
		this.isWriteMetaData = false;
		this.outputFileFactory = OutputFileFactory.instance();
		this.processorFactory = DataProcessorFactory.instance();
	}

	private static JsonNode serializeOutputFile(final OutputFile<?> outputFile) {
		ObjectNode node = mapper.createObjectNode();

		node.put(TYPE_KEY, outputFile.getClass().getName());
		node.put(FILENAME_KEY, outputFile.getFileName());
		node.set(FILE_PROCESSORS_KEY, mapper.convertValue(outputFile.getProcessorIds(), JsonNode.class));

		final String separator = outputFile.getSeparator();
		if (separator != DEFAULT_SEPARATOR) {
			node.put(SEPARATOR_KEY, separator);
		}

		return node;
	}

	public static JsonNode serializeProcessor(final DataProcessor<?, ?> dataProcessor) {
		ObjectNode node = mapper.createObjectNode();

		node.put(TYPE_KEY, dataProcessor.getClass().getName());
		node.put(PROCESSORID_KEY, dataProcessor.getId());

		if (dataProcessor.getAttributes() != null) {
			node.put(ATTRIBUTESTYPE_KEY, dataProcessor.getAttributes().getClass().getName());

			if (!dataProcessor.getAttributes().getClass().equals(AttributesProcessor.class)) {
				node.set(ATTRIBUTES_KEY, mapper.convertValue(dataProcessor.getAttributes(), JsonNode.class));
			}
		}

		return node;
	}

	public static DataProcessingJsonManager createDefault() {
		try {
			return deserializeFromNode(mapper.convertValue(OutputPresets.getOutputDefinition(), JsonNode.class));
		} catch (JsonProcessingException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static DataProcessingJsonManager deserialize(String json) {
		try {
			JsonNode node = json.isEmpty() ? mapper.createObjectNode() : mapper.readTree(json);
			return deserializeFromNode(node);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static DataProcessingJsonManager deserializeFromNode(JsonNode node) throws JsonProcessingException {
		DataProcessingJsonManager manager = new DataProcessingJsonManager();

		// part 1: output files
		ArrayNode outputFilesArrayNode = (ArrayNode) node.get(FILES_KEY);
		if (outputFilesArrayNode != null)
			for (JsonNode fileNode : outputFilesArrayNode) {
				OutputFileStore fileStore = mapper.treeToValue(fileNode, OutputFileStore.class);
				manager.addOutputFile(fileStore);
			}

		// part 2: processors
		ArrayNode processorsArrayNode = (ArrayNode) node.get(PROCESSORS_KEY);
		if (processorsArrayNode != null)
			for (JsonNode processorNode : processorsArrayNode) {
				DataProcessorStore dataProcessorStore = deserializeProcessorStore(processorNode);
				manager.addProcessor(dataProcessorStore);
			}

		// part 3: timestamp
		JsonNode timestampArrayNode = node.get(TIMESTAMP_KEY);
		if (timestampArrayNode != null) {
			manager.setTimestamped(timestampArrayNode.asBoolean());
		}

		JsonNode writeMetaData = node.get(WRITEMETA_KEY);
		if (writeMetaData != null) {
			manager.setWriteMetaData(writeMetaData.asBoolean());
		}

		return manager;
	}

	public static DataProcessorStore deserializeProcessorStore(JsonNode node) {
		DataProcessorStore store = new DataProcessorStore();

		store.setType(node.get(TYPE_KEY).asText());
		store.setId(node.get(PROCESSORID_KEY).asInt());

		if (node.has(ATTRIBUTESTYPE_KEY)) {
			String attType = node.get(ATTRIBUTESTYPE_KEY).asText();

			if (attType != "") {
				store.setAttributesType(attType);

				try {
					store.setAttributes(mapper.readValue(node.get(ATTRIBUTES_KEY).toString(), mapper.getTypeFactory().constructFromCanonical(attType)));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		return store;
	}

	public List<OutputFile<?>> getOutputFiles() {
		return outputFiles;
	}

	public List<DataProcessor<?, ?>> getDataProcessors() {
		return dataProcessors;
	}

	public void addOutputFile(final OutputFileStore fileStore) {
		// If fileName already exists, change it by removing and readding
		this.outputFiles.removeAll(this.outputFiles.stream().filter(f -> f.getFileName().equals(fileStore.getFilename())).collect(Collectors.toList()));
		this.outputFiles.add(instantiateOutputFile(fileStore));
	}

	public OutputFile<?> instantiateOutputFile(final OutputFileStore fileStore) {
		try {
			return outputFileFactory.createOutputfile(fileStore);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public int replaceOutputFile(OutputFileStore fileStore) {
		int index = indexOf(fileStore.getFilename());
		this.outputFiles.remove(index);
		this.outputFiles.add(index, instantiateOutputFile(fileStore));
		return index;
	}

	private int indexOf(String filename) {
		for (int i = 0; i < outputFiles.size(); i++) {
			if (outputFiles.get(i).getFileName().equals(filename)) {
				return i;
			}
		}
		return -1;
	}

	public void addProcessor(final DataProcessorStore dataProcessorStore) {
		DataProcessor<?, ?> dataProcessor = null;
		try {
			dataProcessor = processorFactory.createDataProcessor(dataProcessorStore);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		this.dataProcessors.add(dataProcessor);
	}

	public void addInstantiatedProcessor(final DataProcessor<?, ?> dataProcessor) {
		this.dataProcessors.add(dataProcessor);
	}

	public void updateDataProcessor(final DataProcessor<?, ?> oldDataProcessor, final DataProcessorStore newDataProcessorStore) {
		this.dataProcessors.remove(oldDataProcessor);
		addProcessor(newDataProcessorStore);
	}

	public boolean isTimestamped() {
		return this.isTimestamped;
	}

	public boolean isWriteMetaData(){
		return this.isWriteMetaData;
	}

	public void setTimestamped(boolean isTimestamped) {
		this.isTimestamped = isTimestamped;
	}

	public void setWriteMetaData(boolean isWriteMetaData){
		this.isWriteMetaData = isWriteMetaData;
	}

	public String serialize() throws JsonProcessingException {
		return writer.writeValueAsString(serializeToNode());
	}

	public JsonNode serializeToNode() {
		ObjectNode main = mapper.createObjectNode();

		// part 1: output files
		ArrayNode outputFilesArrayNode = mapper.createArrayNode();
		this.outputFiles.forEach(file -> {
			outputFilesArrayNode.add(serializeOutputFile(file));
		});
		main.set(FILES_KEY, outputFilesArrayNode);

		// part 2: processors
		ArrayNode processorsArrayNode = mapper.createArrayNode();
		this.dataProcessors.forEach(proc -> {
			processorsArrayNode.add(serializeProcessor(proc));
		});
		main.set(PROCESSORS_KEY, processorsArrayNode);

		// part 3: timestamp + write meta data option
		main.put(TIMESTAMP_KEY, this.isTimestamped);
		main.put(WRITEMETA_KEY, this.isWriteMetaData);

		return main;
	}

	public ProcessorManager createProcessorManager(MainModel mainModel, final Topography topography) {
		// this function is called when the simulation starts running

		for (OutputFile f : outputFiles) {
			f.setWriteMetaData(isWriteMetaData()); // allow to write meta data
		}

		return new ProcessorManager(dataProcessors, outputFiles, mainModel, topography);

	}

	public int getMaxProcessorsId() {
		int maxId = 0;
		for (DataProcessor<?, ?> dataProc : dataProcessors) {
			if (dataProc.getId() > maxId) {
				maxId = dataProc.getId();
			}
		}
		return maxId;
	}

	public boolean containsOutputFile(String name){
		return  this.outputFiles.stream().anyMatch(f -> f.getFileName().equals(name));
	}

}
