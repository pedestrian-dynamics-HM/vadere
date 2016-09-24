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
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.projects.dataprocessing.store.DataProcessorStore;
import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.util.reflection.DynamicClassInstantiator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mario Teixeira Parente
 *
 */

public class DataProcessingJsonManager {

    //TODO Change to 'dataprocessing'
    public static final String DATAPROCCESSING_KEY = "processWriters";

    public static final String TRAJECTORIES_FILENAME = "postvis.trajectories";

    public static final String FILES_KEY = "files";
    private static final String TYPE_KEY = "type";
    private static final String FILENAME_KEY = "filename";
    private static final String FILE_PROCESSORS_KEY = "processors";
    private static final String SEPARATOR_KEY = "separator";

    public static final String PROCESSORS_KEY = "processors";
    private static final String PROCESSORID_KEY = "id";
    private static final String ATTRIBUTESTYPE_KEY = "attributesType";
    public static final String ATTRIBUTES_KEY = "attributes";

    public static final String DEFAULT_SEPARATOR = " ";
    public static final String DEFAULT_OUTPUTFILE_TYPE = NoDataKeyOutputFile.class.getName();

    private static ObjectMapper mapper;
    public static ObjectWriter writer;

    private static final DynamicClassInstantiator<OutputFile<?>> outputFileInstantiator;
    private static final DynamicClassInstantiator<DataProcessor<?, ?>> processorInstantiator;

    private List<OutputFile<?>> outputFiles;
    private List<DataProcessor<?, ?>> dataProcessors;

    static {
        mapper = JsonConverter.getMapper();
        writer = mapper.writerWithDefaultPrettyPrinter();
        
        outputFileInstantiator = new DynamicClassInstantiator<>();
        processorInstantiator = new DynamicClassInstantiator<>();
    }

    public DataProcessingJsonManager() {
        this.outputFiles = new ArrayList<>();
        this.dataProcessors = new ArrayList<>();
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

    private OutputFile<?> instantiateOutputFile(final OutputFileStore fileStore) {
        OutputFile<?> file = outputFileInstantiator.createObject(fileStore.getType());
        file.setFileName(fileStore.getFilename());
        file.setProcessorIds(fileStore.getProcessors());
        file.setSeparator(fileStore.getSeparator());
        return file;
    }

    public int replaceOutputFile(OutputFileStore fileStore) {
        int index = indexOf(fileStore.getFilename());
        this.outputFiles.remove(index);
        this.outputFiles.add(index, instantiateOutputFile(fileStore));
        return index;
    }

    private int indexOf(String filename) {
        for (int i = 0; i < outputFiles.size(); i ++) {
            if (outputFiles.get(i).getFileName().equals(filename)) {
                return i;
            }
        }
        return -1;
    }

    public void addProcessor(final DataProcessorStore dataProcessorStore) {
        DataProcessor<?, ?> dataProcessor = processorInstantiator.createObject(dataProcessorStore.getType());
        dataProcessor.setId(dataProcessorStore.getId());
        dataProcessor.setAttributes(dataProcessorStore.getAttributes());
        this.dataProcessors.add(dataProcessor);
    }

    public void updateDataProcessor(final DataProcessor oldDataProcessor, final DataProcessorStore newDataProcessorStore) {
        this.dataProcessors.remove(oldDataProcessor);
        addProcessor(newDataProcessorStore);
    }

    private static JsonNode serializeOutputFile(final OutputFile outputFile) {
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

    public static JsonNode serializeProcessor(final DataProcessor dataProcessor) {
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

    public String serialize() throws JsonProcessingException {
        return writer.writeValueAsString(serializeToNode());
    }

    public JsonNode serializeToNode() {
        ObjectNode main = mapper.createObjectNode();

        ArrayNode outputFilesArrayNode = mapper.createArrayNode();
        ArrayNode processorsArrayNode = mapper.createArrayNode();

        // part 1: output files
        this.outputFiles.forEach(file -> {
            outputFilesArrayNode.add(serializeOutputFile(file));
        });

        // part 2: processor
        this.dataProcessors.forEach(proc -> {
            processorsArrayNode.add(serializeProcessor(proc));
        });

        main.set(FILES_KEY, outputFilesArrayNode);
        main.set(PROCESSORS_KEY, processorsArrayNode);

        return main;
    }

    public static DataProcessingJsonManager createDefault() {
        try {
            return deserializeFromNode(mapper.convertValue(OutputPresets.getOutputDefinition(), JsonNode.class));
        }
        catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static DataProcessingJsonManager deserialize(String json) {
        try {
            JsonNode node = json.isEmpty() ? mapper.createObjectNode() : mapper.readTree(json);
            return deserializeFromNode(node);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static DataProcessingJsonManager deserializeFromNode(JsonNode node) throws JsonProcessingException {
        DataProcessingJsonManager manager = new DataProcessingJsonManager();

        ArrayNode outputFilesArrayNode = (ArrayNode) node.get(FILES_KEY);
        ArrayNode processorsArrayNode = (ArrayNode) node.get(PROCESSORS_KEY);

        // part 1: output files
        if (outputFilesArrayNode != null)
            for (JsonNode fileNode : outputFilesArrayNode) {
                OutputFileStore fileStore = mapper.treeToValue(fileNode, OutputFileStore.class);
                manager.addOutputFile(fileStore);
            }

        // part 2: processor
        if (processorsArrayNode != null)
            for (JsonNode processorNode : processorsArrayNode) {
                DataProcessorStore dataProcessorStore = deserializeProcessorStore(processorNode);
                manager.addProcessor(dataProcessorStore);
            }

        return manager;
    }

    public static DataProcessorStore deserializeProcessorStore(JsonNode node) {
        DataProcessorStore store = new DataProcessorStore();

        store.setType(node.get(TYPE_KEY).asText());
        store.setId(node.get(PROCESSORID_KEY).asInt());

        if(node.has(ATTRIBUTESTYPE_KEY)) {
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

    public ProcessorManager createProcessorManager(MainModel mainModel) {
        return new ProcessorManager(this, this.dataProcessors, this.outputFiles, mainModel);
    }
}
