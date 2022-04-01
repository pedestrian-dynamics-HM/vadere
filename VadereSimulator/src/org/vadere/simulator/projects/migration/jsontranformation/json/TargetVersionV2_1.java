package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.AttributesAirTransmissionModelAerosolCloud;
import org.vadere.state.attributes.models.infection.AttributesAirTransmissionModelDroplets;
import org.vadere.state.attributes.models.infection.AttributesThresholdResponseModel;
import org.vadere.util.version.Version;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Replace submodels under "org.vadere.simulator.models.infection" and attributes under "state.attributes.models.infection"
 *
 * Old scenario description looks like this:
 * <pre>
 * {
 *    "name" : "ScenarioName",
 *    ...
 *    "scenario" : {
 *      "mainModel": ...,
 *      "attributesModel": {
 *        "org.vadere.state.attributes.models...." : {
 *          ...
 *          "submodels" : [ "org.vadere.simulator.models.sir.TransmissionModel" ]
 *          },
 *          ...
 *          "org.vadere.state.attributes.models.AttributesTransmissionModel" : {
 *            "transmissionModelSourceParameters" : [ {
 *              "sourceId" : -1,
 *              "infectionStatus" : "SUSCEPTIBLE"
 *            } ],
 *            "pedestrianRespiratoryCyclePeriod" : 4.0,
 *            "pedestrianPathogenEmissionCapacity" : 4.0,
 *            "pedestrianPathogenAbsorptionRate" : 5.0E-4,
 *            "pedestrianMinInfectiousDose" : 3200.0,
 *            "exposedPeriod" : 432000.0,
 *            "infectiousPeriod" : 1209600.0,
 *            "recoveredPeriod" : 1.296E7,
 *            "aerosolCloudHalfLife" : 600.0,
 *            "aerosolCloudInitialRadius" : 1.5,
 *            "dropletsExhalationFrequency" : 0.0,
 *            "dropletsDistanceOfSpread" : 1.5,
 *            "dropletsAngleOfSpreadInDeg" : 30.0,
 *            "dropletsLifeTime" : 1.001,
 *            "dropletsPathogenLoadFactor" : 200.0
 *          }
 *      },
 *     ...
 *   }
 * }
 * </pre>
 *
 * This migration transforms it to:
 * <pre>
 * {
 *    "name" : "ScenarioName",
 *    ...
 *    "scenario" : {
 *      "mainModel": ...,
 *      "attributesModel": {
 *        "org.vadere.state.attributes.models...." : {
 *          ...
 *          "submodels" : [ "org.vadere.simulator.models.infection.AirTransmissionModel", "org.vadere.simulator.models.infection.ThresholdResponseModel" ]
 *          },
 *          ...
 *          "org.vadere.state.attributes.models.infection.AttributesAirTransmissionModel" : {
 *            "exposureModelSourceParameters" : [ {
 *              "sourceId" : -1,
 *              "infectious" : false
 *            } ],
 *            "infectiousPedestrianIdsNoSource" : [ ],
 *            "pedestrianRespiratoryCyclePeriod" : 4.0,
 *            "aerosolCloudsActive" : true,
 *            "aerosolCloudParameters" : {
 *              "halfLife" : 600.0,
 *              "initialRadius" : 1.5,
 *              "initialPathogenLoad" : 10000.0,
 *              "airDispersionFactor" : 0.0,
 *              "pedestrianDispersionWeight" : 0.0125,
 *              "absorptionRate" : 5.0E-4
 *            },
 *            "dropletsActive" : false,
 *            "dropletParameters" : {
 *              "emissionFrequency" : 0.016666666666666666,
 *              "distanceOfSpread" : 1.5,
 *              "angleOfSpreadInDeg" : 30.0,
 *              "lifeTime" : 1.5,
 *              "pathogenLoad" : 10000.0,
 *              "absorptionRate" : 0.1
 *            }
 *          },
 *          "org.vadere.state.attributes.models.infection.AttributesThresholdResponseModel" : {
 *            "exposureToInfectedThreshold" : 1000.0
 *          }
 *      },
 *      ...
 *    }
 * }
 * </pre>
 *
 *
 * Replace field healthStatus and add field infectionStatus under node dynamicElements. Assign value null in both cases.<p>
 *
 *
 * Further, this handles deprecated data writers:<p>
 * Remove:
 * <ul>
 * <li>NumberOfPedPerInfectionStatusProcessor</li>
 * <li>PedestrianHealthStatusProcessor</li>
 * </ul>
 * Rename:
 * <ul>
 * <li>AerosolCloudShapeProcessor -> AerosolCloudDataProcessor</li>
 * <li>AttributesAerosolCloudShapeProcessor -> AttributesAerosolCloudDataProcessor</li>
 * <li>Output file: aerosolCloudShapes.txt -> aerosolCloudData.txt</li>
 * <li>PedestrianPathogenLoadProcessor -> PedestrianDegreeOfExposureProcessor.</li>
 * </ul>
 *
 */
@MigrationTransformation(targetVersionLabel = "2.1")
public class TargetVersionV2_1 extends SimpleJsonTransformation {
    public TargetVersionV2_1() {
        super(Version.V2_1);
    }

    @Override
    protected void initDefaultHooks() {
        addPostHookFirst(this::renameProcessWriters);
        addPostHookFirst(this::replaceSubmodelTransmissionModelUnderAttributesModel);
        addPostHookFirst(this::replaceAttributesTransmissionModelUnderAttributesModel);
        addPostHookFirst(this::replaceDeserializedHealthStatusUnderDynamicElements);
        addPostHookLast(this::sort);
    }

    private JsonNode replaceDeserializedHealthStatusUnderDynamicElements(JsonNode node) throws MigrationException {

        ArrayNode dynamicElementsNode = (ArrayNode) path(node, "scenario/topography/dynamicElements");

        if (!dynamicElementsNode.isMissingNode()) {
            for (int i = 0; i < dynamicElementsNode.size(); i++) {
                JsonNode attributesNode = dynamicElementsNode.get(i);
                if (!path(attributesNode, "healthStatus").isMissingNode()) {
                    remove(attributesNode, "healthStatus");
                    ((ObjectNode) attributesNode).set("healthStatus", null);
                    ((ObjectNode) attributesNode).set("infectionStatus", null);
                }
            }
        }

        return node;
    }

    private JsonNode renameProcessWriters(JsonNode scenarioFile) throws MigrationException {
        // 1) rename
        renameProcessorWriters(scenarioFile);

        // 2) remove processors and files that use these processors
        String[] processorsToRemove = {
                "org.vadere.simulator.projects.dataprocessing.processor.NumberOfPedPerInfectionStatusProcessor",
                "org.vadere.simulator.projects.dataprocessing.processor.PedestrianHealthStatusProcessor"
        };
        List<Integer> processorIds = getProcessorIdsByType(scenarioFile, processorsToRemove);
        removeFilesByProcessorId(scenarioFile, processorIds);
        removeProcessorsByType(scenarioFile, processorsToRemove);

        return scenarioFile;
    }

    private void removeFilesByProcessorId(JsonNode scenarioFile, List<Integer> processorIds) throws MigrationException {
        ArrayNode filesArray = (ArrayNode) path(scenarioFile, "processWriters/files");
        Set<Integer> filesIndicesToRemove = new TreeSet<>();
        for (int fileIndex = 0; fileIndex < filesArray.size(); fileIndex++) {
            for (int processorIndex = 0; processorIndex < filesArray.get(fileIndex).get("processors").size(); processorIndex++) {
                if (processorIds.contains(filesArray.get(fileIndex).get("processors").get(processorIndex).asInt())) {
                    filesIndicesToRemove.add(fileIndex);
                }
            }
        }

        // sort in descending order since children are removed by index, but indices are updated after each removal
        for (int i : filesIndicesToRemove.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
            filesArray.remove(i);
        }

    }

    private void removeProcessorsByType(JsonNode scenarioFile, String[] processorsTypesToRemove) throws MigrationException {
        ArrayNode processorsArray = (ArrayNode) path(scenarioFile, "processWriters/processors");
        Set<Integer> processorsIndicesToRemove = new TreeSet<>();
        Set<Integer>  processorIdsToRemove = new HashSet<>();
        for (String type : processorsTypesToRemove) {
            ArrayList<JsonNode> processorList = getProcessorsByType(scenarioFile, type);
            for (JsonNode processor : processorList) {
                processorIdsToRemove.add(processor.get("id").asInt());
            }
        }

        for (int processorIndex = 0; processorIndex < processorsArray.size(); processorIndex++) {
            if (processorIdsToRemove.contains(processorsArray.get(processorIndex).get("id").asInt())) {
                processorsIndicesToRemove.add(processorIndex);
            }
        }

        // sort in descending order since children are removed by index, but indices are updated after each removal
        for (int i : processorsIndicesToRemove.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
            processorsArray.remove(i);
        }
    }

    @NotNull
    private List<Integer> getProcessorIdsByType(JsonNode scenarioFile, String[] processorTypes) throws MigrationException {
        List<Integer> processorIds = new ArrayList<>();

        for (String type : processorTypes) {
            ArrayList<JsonNode> processorList = getProcessorsByType(scenarioFile, type);
            for (JsonNode processor : processorList) {
                processorIds.add(processor.get("id").asInt());
            }
        }
        return processorIds;
    }

    private void renameProcessorWriters(JsonNode scenarioFile) throws MigrationException {
        renameProcessorFieldName(scenarioFile, "type", "PedestrianPathogenLoadProcessor", "PedestrianDegreeOfExposureProcessor");
        renameProcessorFieldName(scenarioFile, "type", "AerosolCloudShapeProcessor", "AerosolCloudDataProcessor");
        renameProcessorFieldName(scenarioFile, "attributesType", "AttributesAerosolCloudShapeProcessor", "AttributesAerosolCloudDataProcessor");
        renameFileFieldName(scenarioFile, "filename", "aerosolCloudShapes", "aerosolCloudData");
    }

    /**
     * Splits old model into two models and removes the old model
     */
    private JsonNode replaceSubmodelTransmissionModelUnderAttributesModel(JsonNode scenarioFile) throws MigrationException {
        String oldName = "org.vadere.simulator.models.sir.TransmissionModel";
        String newName1 = "org.vadere.simulator.models.infection.AirTransmissionModel";
        String newName2 = "org.vadere.simulator.models.infection.ThresholdResponseModel";

        JsonNode submodelsNode = path(scenarioFile, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM/submodels");

        if (!submodelsNode.isMissingNode()) {
            ArrayNode submodelsArray = (ArrayNode) submodelsNode;
            for (int i = 0; i < submodelsArray.size(); i++) {
                if (submodelsArray.get(i).asText().equals(oldName)) {
                    submodelsArray.remove(i);
                    submodelsArray.add(newName1);
                    submodelsArray.add(newName2);
                }
            }
        }

        return scenarioFile;
    }

    private JsonNode replaceAttributesTransmissionModelUnderAttributesModel(JsonNode scenarioFile) throws MigrationException {
        String path = "scenario/attributesModel";

        JsonNode attrModelNode = path(scenarioFile, path);
        if (!attrModelNode.isMissingNode()) {

            String oldAttributesName = "org.vadere.state.attributes.models.AttributesTransmissionModel";

            JsonNode attrTransModelNode = path(scenarioFile, path + "/" + oldAttributesName);
            if (!attrTransModelNode.isMissingNode()) {

                // 1) rename children
                renameTransmissionModelSourceParametersUnderAttributesTransmissionModel(attrTransModelNode);

                // 2) reorder / rename fields
                HashMap<String, Double> tempData = new HashMap<>();
                List<String> tempDataKeys = Arrays.asList("pedestrianPathogenEmissionCapacity",
                        "pedestrianPathogenAbsorptionRate",
                        "pedestrianMinInfectiousDose",
                        "aerosolCloudHalfLife",
                        "aerosolCloudInitialArea",
                        "aerosolCloudInitialRadius",
                        "dropletsExhalationFrequency",
                        "dropletsDistanceOfSpread",
                        "dropletsAngleOfSpreadInDeg",
                        "dropletsLifeTime",
                        "dropletsPathogenLoadFactor"
                );
                replaceChildrenUnderAttributesTransmissionModel(attrTransModelNode, tempData, tempDataKeys);

                // 3) remove children whose information is not further needed
                List<String> keysToRemove = Arrays.asList("exposedPeriod",
                        "infectiousPeriod",
                        "recoveredPeriod");
                removeChildrenUnderAttributesTransmissionModel((ObjectNode) attrTransModelNode, keysToRemove);

                // 4) add new children
                addNewChildren((ObjectNode) attrTransModelNode);

                // 5) rename parent
                renameField((ObjectNode) attrModelNode, oldAttributesName, "org.vadere.state.attributes.models.infection.AttributesAirTransmissionModel");

                // 6) add ThresholdResponseModelAttributes
                addThresholdResponseModelUnderAttributesModel(attrModelNode, tempData.get("pedestrianMinInfectiousDose"));
            }
        }

        return scenarioFile;
    }

    private void addNewChildren(ObjectNode attrTransModelNode) {
        attrTransModelNode.put("infectiousPedestrianIdsNoSource", new ArrayNode(JsonNodeFactory.instance));
    }

    private void replaceChildrenUnderAttributesTransmissionModel(JsonNode attrTransModelNode, HashMap<String, Double> tempData, List<String> tempDataKeys) {
        // 1) collect temp data from JSON
        for (String key : tempDataKeys) {
            if (attrTransModelNode.has(key)) {
                tempData.put(key, attrTransModelNode.get(key).asDouble());
            } else {
                tempData.put(key, null);
            }
        }

        // 2) add children with data from previous JSON
        addAerosolCloudParametersUnderAttributesTransmissionModel((ObjectNode) attrTransModelNode, tempData);
        addDropletParametersUnderAttributesTransmissionModel((ObjectNode) attrTransModelNode, tempData);

        // 3) remove duplicated children
        removeChildrenUnderAttributesTransmissionModel((ObjectNode) attrTransModelNode, tempDataKeys);
    }

    private void removeChildrenUnderAttributesTransmissionModel(ObjectNode attributesNode, List<String> tempDataKeys) {
        attributesNode.remove(tempDataKeys);
    }

    private void addAerosolCloudParametersUnderAttributesTransmissionModel(ObjectNode attributesNode, HashMap<String, Double> tempData) {
        attributesNode.put("aerosolCloudsActive", (tempData.get("pedestrianPathogenEmissionCapacity") > 0));

        double initialRadius = Math.round(((tempData.get("aerosolCloudInitialRadius") != null) ? tempData.get("aerosolCloudInitialRadius") : Math.sqrt(tempData.get("aerosolCloudInitialArea") / Math.PI)) * 10.0) / 10.0;

        AttributesAirTransmissionModelAerosolCloud aerosolCloudParameters = new AttributesAirTransmissionModelAerosolCloud(
                tempData.get("aerosolCloudHalfLife"),
                initialRadius,
                Math.pow(10, tempData.get("pedestrianPathogenEmissionCapacity")),
                0,
                0.0125,
                tempData.get("pedestrianPathogenAbsorptionRate")
        );

        addAttributesObjectToNode(attributesNode, aerosolCloudParameters, "aerosolCloudParameters");
    }

    private void addDropletParametersUnderAttributesTransmissionModel(ObjectNode attributesNode, HashMap<String, Double> tempData) {
        attributesNode.put("dropletsActive", (tempData.get("dropletsExhalationFrequency") > 0));
        AttributesAirTransmissionModelDroplets dropletParameters = new AttributesAirTransmissionModelDroplets(
                ((tempData.get("dropletsExhalationFrequency") > 0) ? tempData.get("dropletsExhalationFrequency") : 0.167),
                tempData.get("dropletsDistanceOfSpread"),
                tempData.get("dropletsAngleOfSpreadInDeg"),
                tempData.get("dropletsLifeTime"),
                tempData.get("dropletsPathogenLoadFactor") * tempData.get("pedestrianPathogenEmissionCapacity"),
                0.1
        );

        addAttributesObjectToNode(attributesNode, dropletParameters, "dropletParameters");
    }

    private void addThresholdResponseModelUnderAttributesModel(JsonNode node, double threshold) {
        AttributesThresholdResponseModel attributes = new AttributesThresholdResponseModel(threshold);

        addAttributesObjectToNode((ObjectNode) node, attributes, "org.vadere.state.attributes.models.infection.AttributesThresholdResponseModel");
    }

    private void addAttributesObjectToNode(ObjectNode node, Attributes attributes, String attributesKey) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode attributesNode = mapper.convertValue(attributes, ObjectNode.class);
        node.set(attributesKey, attributesNode);
    }

    private void renameTransmissionModelSourceParametersUnderAttributesTransmissionModel(JsonNode attributesNode) {
        String oldName = "transmissionModelSourceParameters";
        String newName = "exposureModelSourceParameters";

        // first replace children
        ArrayNode sourceParamsNode = (ArrayNode) path(attributesNode, oldName);
        replaceFieldInfectionStatusUnderTransmissionModelSourceParameters(sourceParamsNode);
        // then rename parent
        renameField((ObjectNode) attributesNode, oldName, newName);
    }

    private void replaceFieldInfectionStatusUnderTransmissionModelSourceParameters(ArrayNode sourceParamsArray) {
        String oldKey = "infectionStatus";
        String newKey = "infectious";

        if (sourceParamsArray.isArray() && sourceParamsArray.size() > 0) {
            for (int i = 0; i < sourceParamsArray.size(); i++) {
                ObjectNode child = (ObjectNode) sourceParamsArray.get(i);
                if (child.get(oldKey).asText().equals("INFECTIOUS")) {
                    child.put(newKey, true);
                } else {
                    child.put(newKey, false);
                }
                child.remove(oldKey);
            }
        }
    }
}
