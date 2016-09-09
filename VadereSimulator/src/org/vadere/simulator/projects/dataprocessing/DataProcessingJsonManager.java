package org.vadere.simulator.projects.dataprocessing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;
import org.vadere.simulator.projects.dataprocessing.processor.Processor;
import org.vadere.simulator.projects.dataprocessing.store.OutputFileStore;
import org.vadere.simulator.projects.dataprocessing.store.ProcessorStore;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.util.reflection.DynamicClassInstantiator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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

    public static final String ATTRIBUTES_KEY = "attributes";

    public static final String DEFAULT_SEPARATOR = " ";

    private static ObjectMapper mapper;
    public static ObjectWriter writer;

    private static final DynamicClassInstantiator<OutputFile<?>> outputFileInstantiator;
    private static final DynamicClassInstantiator<Processor<?, ?>> processorInstantiator;
    private static final DynamicClassInstantiator<AttributesProcessor> attributesInstantiator;

    private List<OutputFile<?>> outputFiles;
    private List<Processor<?, ?>> processors;
    private List<AttributesProcessor> attributes;

    static {
        mapper = JsonConverter.getMapper();
        writer = mapper.writerWithDefaultPrettyPrinter();

        outputFileInstantiator = new DynamicClassInstantiator<>();
        processorInstantiator = new DynamicClassInstantiator<>();
        attributesInstantiator = new DynamicClassInstantiator<>();
    }

    public DataProcessingJsonManager() {
        this.outputFiles = new ArrayList<>();
        this.processors = new ArrayList<>();
        this.attributes = new ArrayList<>();
    }

    public void addOutputFile(final OutputFileStore fileStore) {
        // If fileName already exists, change it by removing and readding
        this.outputFiles.removeAll(this.outputFiles.stream().filter(f -> f.getFileName().equals(fileStore.getFilename())).collect(Collectors.toList()));

        OutputFile<?> file = outputFileInstantiator.createObject(fileStore.getType());
        file.setFileName(fileStore.getFilename());
        file.setProcessorIds(fileStore.getProcessors());
        file.setSeparator(fileStore.getSeparator());
        this.outputFiles.add(file);
    }

    public void addProcessor(final ProcessorStore processorStore) {
        Processor<?, ?> processor = processorInstantiator.createObject(processorStore.getType());
        processor.setId(processorStore.getId());
        this.processors.add(processor);
    }

    public void addAttributes(final AttributesProcessor attributes) {
        this.attributes.add(attributes);
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

    private static JsonNode serializeProcessor(final Processor processor) {
        ObjectNode node = mapper.createObjectNode();

        node.put(TYPE_KEY, processor.getClass().getName());
        node.put(PROCESSORID_KEY, processor.getId());

        return node;
    }

    private static JsonNode serializeAttributesProcessor(final AttributesProcessor attributes) {
        return mapper.convertValue(attributes, JsonNode.class);
    }

    public String serialize() throws JsonProcessingException {
        return writer.writeValueAsString(serializeToNode());
    }

    public JsonNode serializeToNode() {
        ObjectNode main = mapper.createObjectNode();

        ArrayNode outputFilesArrayNode = mapper.createArrayNode();
        ArrayNode processorsArrayNode = mapper.createArrayNode();
        ObjectNode attributesNode = mapper.createObjectNode();

        // part 1: output files
        this.outputFiles.forEach(file -> {
            outputFilesArrayNode.add(serializeOutputFile(file));
        });

        // part 2: processor
        this.processors.forEach(proc -> {
            processorsArrayNode.add(serializeProcessor(proc));
        });

        // part 3: attributes
        this.attributes.forEach(att -> {
            attributesNode.set(att.getClass().getName(), serializeAttributesProcessor(att));
        });

        main.set(FILES_KEY, outputFilesArrayNode);
        main.set(PROCESSORS_KEY, processorsArrayNode);
        main.set(ATTRIBUTES_KEY, attributesNode);

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
        JsonNode attributesNode = node.get(ATTRIBUTES_KEY);

        // part 1: output files
        if (outputFilesArrayNode != null)
            for (JsonNode fileNode : outputFilesArrayNode) {
                OutputFileStore fileStore = mapper.treeToValue(fileNode, OutputFileStore.class);
                manager.addOutputFile(fileStore);
            }

        // part 2: processor
        if (processorsArrayNode != null)
            for (JsonNode processorNode : processorsArrayNode) {
                ProcessorStore processorStore = mapper.treeToValue(processorNode, ProcessorStore.class);
                manager.addProcessor(processorStore);
            }

        // part 3: attributes
        if (attributesNode != null) {
            Iterator<String> it = attributesNode.fieldNames();
            while (it.hasNext()) {
                String fieldName = it.next();
                JsonNode attributeNode = attributesNode.get(fieldName);
                AttributesProcessor attributes = mapper.treeToValue(attributeNode, attributesInstantiator.getClassFromName(fieldName));
                manager.addAttributes(attributes);
            }
        }

        return manager;
    }

    public ProcessorManager createProcessorManager() {
        return new ProcessorManager(this, this.processors, this.attributes, this.outputFiles);
    }
}
