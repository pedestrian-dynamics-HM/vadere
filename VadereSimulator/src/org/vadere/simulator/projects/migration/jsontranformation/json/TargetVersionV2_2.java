package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.control.psychology.perception.models.PerceptionModel;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.attributes.AttributesPsychologyLayer;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.util.version.Version;


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
        addPostHookLast(this::sort);
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

        String stimulusInfos = "/scenario/stimulusInfos";

        ArrayNode psychologyLayer = (ArrayNode) node.at(stimulusInfos);

        for (JsonNode entry : psychologyLayer){
            ArrayNode stimuli = (ArrayNode) entry.get("stimuli");
            for (JsonNode stimulus : stimuli){
                ((ObjectNode) stimulus).remove("id");
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

        String path = "org.vadere.state.attributes.models.psychology."+ key +".Attributes";

        return path + psychologyLayer.get(key).toString().replace("\"", "");
    }



}
