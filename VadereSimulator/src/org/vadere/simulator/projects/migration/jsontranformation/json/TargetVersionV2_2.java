package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.version.Version;

import java.util.Iterator;


@MigrationTransformation(targetVersionLabel = "2.2")
public class TargetVersionV2_2 extends SimpleJsonTransformation {

    JacksonObjectMapper mapper = new JacksonObjectMapper();

    public TargetVersionV2_2() {
        super(Version.V2_2);
    }

    @Override
    protected void initDefaultHooks() {
        addPostHookFirst(this::addPsychologyLayerNodeIfMissing);
        addPostHookLast(this::addNestedModelAttributesKeyInPsychologyLayer);
        addPostHookLast(this::removeReactionProbabilities);
        addPostHookLast(this::removeStimulusIds);
        addPostHookLast(this::moveThreatWaitAreasToLocation);
        addPostHookLast(this::sort);
    }



    private JsonNode moveThreatWaitAreasToLocation(JsonNode node) {

        // generates one stimulusInfo for each stimulus

        String key = "stimulusInfos";
        JsonNode scenarioNode = node.get("scenario");
        ArrayNode newStimulusInfos = mapper.createArrayNode();

        if (!path(scenarioNode, "stimulusInfos").isMissingNode()) {

            ArrayNode psychologyLayer = (ArrayNode) scenarioNode.get(key);

            for (JsonNode stimulusInfoJson : psychologyLayer.deepCopy()) {

                ObjectNode stimulusInfo = stimulusInfoJson.deepCopy();
                ArrayNode stimuli = (ArrayNode) stimulusInfo.get("stimuli");

                for (JsonNode stimulusJson : stimuli) {

                    ArrayNode stimuliList = mapper.createArrayNode();
                    ObjectNode stimulusAdjusted = mapper.createObjectNode();

                    ArrayNode areaList = mapper.createArrayNode();
                    ObjectNode location = mapper.createObjectNode();


                    ObjectNode stimulus = (ObjectNode) stimulusJson;
                    String stimulusType = stimulus.get("type").toString().replace("\"", "");

                    if (stimulusType.equals("WaitInArea")) {

                        // Step 1: create perception area definition
                        // move area definition to new location attribute.
                        areaList.add(stimulus.get("area").deepCopy());

                        // remove perception area definition from stimulus
                        // replace WaitInArea by simple Wait.
                        stimulusAdjusted.put("type", "Wait");
                        stimuliList.add(stimulusAdjusted);


                    } else if (stimulusType.equals("Threat")){
                        // separate perception area and threat origin.
                        // After the migration: perception area can be of arbitrary shape.
                        // Before the migration, the perception area has always been a disk with the following properties:
                        //      center = target centroid (corresponding target is specified using "originAsTargetId" in Threat.class)
                        //      radius = radius (old attribute in Threat.class)
                        // In the migration, I add the corresponding perception area (disk) to the location attribute.


                        // Step 1: create perception area definition
                        // generate new disk definition and place it under location
                        JsonNode disk = null;
                        boolean targetIdFound = false;
                        Iterator<JsonNode> iter = scenarioNode.get("topography").get("targets").iterator();
                        while (!targetIdFound && iter.hasNext()){

                            JsonNode target = iter.next();
                            if (target.get("id") == stimulus.get("originAsTargetId")){
                                try {
                                    VShape shape = mapper.treeToValue(target.get("shape"), VShape.class);
                                    double radius = stimulus.get("radius").asDouble();
                                    VCircle circle = new VCircle(shape.getCentroid(), radius);
                                    disk = mapper.convertValue(circle, JsonNode.class);
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                                targetIdFound = true;
                            }
                        }
                        if (disk == null){
                            throw new RuntimeException("Threat with originAsTargetId="
                                    + stimulus.get("originAsTargetId").asInt()
                                    + ": target does not exist.");
                        }
                        areaList.add(disk);

                        // remove perception area definition from stimulus
                        stimulusAdjusted = stimulus.deepCopy();
                        stimulusAdjusted.remove("radius"); // no longer necessary, see disk definition
                        stimuliList.add(stimulusAdjusted);


                    } else {
                        stimuliList.add(stimulus);
                    }

                    location.put("areas", areaList);
                    stimulusInfo.put("location", location);

                    stimulusInfo.remove("stimuli");
                    stimulusInfo.put("stimuli", stimuliList);
                    newStimulusInfos.add(stimulusInfo);
                }
            }
        }


        ((ObjectNode) scenarioNode).remove("stimulusInfos");
        ((ObjectNode) scenarioNode).put("stimulusInfos", newStimulusInfos);

        return node;

    }


    private JsonNode removeReactionProbabilities(JsonNode node) {
        String key = "reactionProbabilities";
        JsonNode scenarioNode = node.get("scenario");

        if (!path(scenarioNode, key).isMissingNode()) {
            ((ObjectNode) scenarioNode).remove(key);
        }

        return node;
    }

    private JsonNode removeStimulusIds(JsonNode node) {

        String key = "stimulusInfos";
        JsonNode scenarioNode = node.get("scenario");

        if (!path(scenarioNode, "stimulusInfos").isMissingNode()) {

            ArrayNode psychologyLayer = (ArrayNode) scenarioNode.get(key);

            for (JsonNode entry : psychologyLayer) {
                ArrayNode stimuli = (ArrayNode) entry.get("stimuli");
                for (JsonNode stimulus : stimuli) {
                    ((ObjectNode) stimulus).remove("id");
                }
            }

        }

        return node;
    }


    private JsonNode addPsychologyLayerNodeIfMissing(JsonNode node) {

        String keyMissing = "psychologyLayer";
        String psychologyLayerKey = "/scenario/attributesPsychology";

        ObjectNode psychologyLayer = (ObjectNode) node.at(psychologyLayerKey);

        if (path(psychologyLayer, keyMissing).isMissingNode()) {
            ObjectNode attributes = mapper.createObjectNode();
            attributes.put("perception", "SimplePerceptionModel"); //AttributesPsychologyLayer.DEFAULT_PERCEPTION_MODEL
            attributes.put("cognition", "SimpleCognitionModel"); //AttributesPsychologyLayer.DEFAULT_COGNITION_MODEL
            psychologyLayer.put(keyMissing, attributes);
        }

        return node;
    }

    private JsonNode addNestedModelAttributesKeyInPsychologyLayer(JsonNode node) throws MigrationException {

        String keyMissing = "attributesModel";
        String psychologyLayerKey = "/scenario/attributesPsychology/psychologyLayer";

        ObjectNode psychologyLayer = (ObjectNode) node.at(psychologyLayerKey);

        if (path(psychologyLayer, keyMissing).isMissingNode()) {
            ObjectNode attributesNode = mapper.createObjectNode();
            attributesNode.put(extracted(psychologyLayer, "perception"), mapper.createObjectNode());
            attributesNode.put(extracted(psychologyLayer, "cognition"), mapper.createObjectNode());

            psychologyLayer.put(keyMissing, attributesNode); // add empty node

        } else {
            throw new MigrationException("Key " + keyMissing + " not allowed under " + psychologyLayerKey + ".");
        }

        return node;
    }

    private String extracted(ObjectNode psychologyLayer, String key) {

        String path = "org.vadere.state.attributes.models.psychology." + key + ".Attributes";
        return path + psychologyLayer.get(key).toString().replace("\"", "");
    }


}
