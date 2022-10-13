package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.scenario.Target;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.util.version.Version;

import java.util.Iterator;

/**
 * Old:
 * {
 *     ...
 *     "waitingTime" : any_double
 * }
 * New:
 * {
 *     "waitingTimeDistribution" : "constant"
 *     "distributionParameters" : {
 *         "updateFrequency" : any_double
 *     }
 * }
 *
 * Old:
 * {
 *     "individualWaiting" : true | false
 * }
 *
 * New:
 * {
 *     "waitingBehaviour": "INDIVIDUAL" | "TRAFFIC_LIGHT" | "NO_WAITING"
 * }
 */
@MigrationTransformation(targetVersionLabel = "2.3")
public class TargetVersionV2_3 extends SimpleJsonTransformation {

    JacksonObjectMapper mapper = new JacksonObjectMapper();

    public TargetVersionV2_3() {
        super(Version.V2_3);
    }

    @Override
    protected void initDefaultHooks() {
        addPostHookLast(this::replaceIndividualWaitingWithWaitingBehaviour);
        addPostHookLast(this::replaceWaitingTimeWithDistribution);
        addPostHookLast(this::sort);
    }

    private JsonNode replaceWaitingTimeWithDistribution(JsonNode node) throws MigrationException {
        if (!path(node, "scenario/topography/targets").isMissingNode()) {
            Iterator<JsonNode> iter = iteratorTargets(node);
            while (iter.hasNext()) {
                JsonNode target = iter.next();

                if (path(target, "waitingTime").isMissingNode() &&
                        !path(target, "waitingTimeDistribution").isMissingNode() &&
                        !path(target, "distributionParameters").isMissingNode()) {
                    continue;
                }
                if (!path(target, "waitingTime").isMissingNode()) {
                    double waitingTime = target.get("waitingTime").asDouble();
                    if (waitingTime == 0.0){
                        ObjectNode objectNode = (ObjectNode) target;
                        objectNode.put("waitingTimeDistribution", "null");
                        objectNode.put("distributionParameters", "null");
                        addStringField(target,"waitingBehaviour", Target.WaitingBehaviour.NO_WAITING.toString());
                    }else{
                        ObjectNode objectNode = (ObjectNode) target;
                        objectNode.put("waitingTimeDistribution", "constant");
                        ObjectNode param = mapper.createObjectNode();
                        param.put("updateFrequency", waitingTime);
                        objectNode.put("distributionParameters", param);
                    }
                    remove(target, "waitingTime");
                }
            }
        }
        return node;
    }

    private JsonNode replaceIndividualWaitingWithWaitingBehaviour(JsonNode node) throws MigrationException {

        if (!path(node, "scenario/topography/targets").isMissingNode()) {
            Iterator<JsonNode> iter = iteratorTargets(node);
            while (iter.hasNext()) {
                JsonNode target = iter.next();

                if (path(target, "individualWaiting").isMissingNode() &&
                        path(target, "waitingBehaviour").isMissingNode()) {
                    ObjectNode objectNode = (ObjectNode) target;
                    objectNode.put("waitingBehaviour", Target.WaitingBehaviour.INDIVIDUAL.toString());
                }
                if (!path(target, "individualWaiting").isMissingNode()) {
                    boolean individual = target.get("individualWaiting").asBoolean();
                    remove(target, "individualWaiting");
                    ObjectNode objectNode = (ObjectNode) target;
                    if (individual) {
                        objectNode.put("waitingBehaviour", Target.WaitingBehaviour.INDIVIDUAL.toString());
                    } else {
                        objectNode.put("waitingBehaviour", Target.WaitingBehaviour.TRAFFIC_LIGHT.toString());
                    }
                }

            }
        }
        return node;
    }
}
