package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
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
        addPostHookLast(this::sort);
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
            psychologyLayer.put(keyMissing, mapper.createObjectNode()); // add empty node
        } else {
            throw new MigrationException("Key " + keyMissing + " not allowed under " + psychologyLayerKey + ".");
        }

        return node;
    }


}
