package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
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

        String key = "stimulusInfos";
        JsonNode scenarioNode = node.get("scenario");

        ArrayNode newStimulusInfos = mapper.createArrayNode();

        if (!path(scenarioNode, "stimulusInfos").isMissingNode()) {

            ArrayNode psychologyLayer = (ArrayNode) scenarioNode.get(key);


            for (JsonNode entry : psychologyLayer.deepCopy()) {
                ArrayNode stimuli = (ArrayNode) entry.get("stimuli");


                for (JsonNode stimusNode : stimuli) {

                    ObjectNode stimulusInfo = (ObjectNode) entry;
                    ObjectNode nodenew = stimulusInfo.deepCopy();

                    ArrayNode sss = mapper.createArrayNode();
                    ObjectNode aa = mapper.createObjectNode();


                    ObjectNode stimulus = (ObjectNode) stimusNode;
                    String stimulusType = stimulus.get("type").toString().replace("\"", "");

                    ArrayNode ss = mapper.createArrayNode();
                    ObjectNode aas = mapper.createObjectNode();

                    if (stimulusType.equals("WaitInArea")) {
                        ObjectNode node1 = (ObjectNode) stimulus.get("area").deepCopy();
                        ss.add(node1);

                        aa.put("type", "Wait");
                        sss.add(aa);


                    } else if (stimulusType.equals("Threat")){


                        double radius = stimulus.get("radius").asDouble();

                        Iterator<JsonNode> iter = scenarioNode.get("topography").get("targets").iterator();

                        boolean isSearchingForTargetId = true;

                        VShape shape = null;
                        JsonNode area = null;

                        while (isSearchingForTargetId && iter.hasNext()){

                            JsonNode targetentry = iter.next();

                            if (targetentry.get("id") == stimulus.get("originAsTargetId")){

                                JsonNode shapeJ = targetentry.get("shape");

                                try {

                                    shape = mapper.treeToValue(shapeJ, VShape.class);
                                    VPoint center;
                                    center = shape.getCentroid();
                                    VCircle circle = new VCircle(center, radius);


                                    area = mapper.convertValue(circle, JsonNode.class);

                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }

                                isSearchingForTargetId = false;
                            }
                        }
                        if (shape == null){
                            throw new RuntimeException("target id not found.");
                        }


                        ss.add(area);


                        aa.put("type", "Threat");
                        aa.put("loudness", stimulus.get("loudness") );
                        aa.put("originAsTargetId", stimulus.get("originAsTargetId") );

                        sss.add(aa);


                    } else {
                        sss.add(stimulus);
                    }

                    aas.put("areas", ss);
                    nodenew.put("location", aas);

                    nodenew.remove("stimuli");
                    nodenew.put("stimuli", sss);
                    newStimulusInfos.add(nodenew);
                }


            }
        }

        ObjectNode node000 = (ObjectNode) scenarioNode;
        node000.remove("stimulusInfos");
        node000.put("stimulusInfos", newStimulusInfos);

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
