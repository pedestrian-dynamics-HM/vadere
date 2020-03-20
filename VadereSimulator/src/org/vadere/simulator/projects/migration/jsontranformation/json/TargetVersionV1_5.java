package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;

/**
 * Old scenario description for events looks like this:
 * <pre>
 *     "eventInfos" : [ {
 *       "eventTimeframe" : {
 *         "startTime" : 0.0,
 *         "endTime" : 0.4,
 *         "repeat" : false,
 *         "waitTimeBetweenRepetition" : 0.0
 *       },
 *       "events" : [ {
 *         "type" : "BangEvent",
 *         "targets" : [ ],
 *         "originAsTargetId" : 1
 *       } ]
 * </pre>
 *
 * This migration shall transform it to:
 * <pre>
 *     "stimulusInfos" : [ {
 *       "timeframe" : {
 *         "startTime" : 0.0,
 *         "endTime" : 0.4,
 *         "repeat" : false,
 *         "waitTimeBetweenRepetition" : 0.0
 *       },
 *       "stimuli" : [ {
 *         "type" : "Bang",
 *         "originAsTargetId" : 1
 *       } ]
 * </pre>
 *
 * Firstly, rename child nodes. Then, rename top-level "eventInfos" node.
 *
 * Also, rename
 * - "useSalientBehavior" to "usePsychologyLayer" under "attributesSimulation" node
 * - "FootStepMostImportantEventProcessor" to "FootStepMostImportantStimulusProcessor"
 * - "FootStepSalientBehaviorProcessor" to "FootStepSelfCategoryProcessor"
 * - "salientBehavior" to "selfCategory" in "dynamicElement" nodes
 *
 * After renaming, encapsulate two psychology-related attributes into
 * a new "psychology" node in "dynamicElement" nodes:
 * - mostImportantStimulus
 * - selfCategory
 *
 * A resctructured dynamic element node looks like this:
 * <pre>
 * {
 *   ...
 *   "psychology" : {
 *     "mostImportantStimulus" : {
 *       "type" : "ElapsedTime"
 *     },
 *     "selfCategory" : "TARGET_ORIENTED"
 *   },
 *   ...
 *   "type" : "PEDESTRIAN"
 * }
 * </pre>
 */
@MigrationTransformation(targetVersionLabel = "1.5")
public class TargetVersionV1_5 extends SimpleJsonTransformation {

	public TargetVersionV1_5(){
		super(Version.V1_5);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::renameEventsToStimuli);
		addPostHookLast(this::sort);
	}

	public JsonNode renameEventsToStimuli(JsonNode node) throws MigrationException {
		renameTimeframes(node);
		removeTargetsFromEventsAndRemoveEventSuffix(node);
		renameEventsArrayToStimuli(node);
		renameEventInfosToStimulusInfos(node);
		renameMostImportantEventInDynamicElements(node);
		renameUseSalientBehavior(node);
		renameOutputProcessorMostImportantEvent(node);
		renameOutputProcessorSalientBehavior(node);
		renameSalientBehaviorInDynamicElements(node);

		createNewPsychologyNodeInDynamicElements(node);

		return node;
	}

	private void renameTimeframes(JsonNode node) throws MigrationException {
		String oldName = "eventTimeframe";
		String newName = "timeframe";

		JsonNode eventInfosNode = path(node, "scenario/eventInfos");

		if (eventInfosNode.isArray()) {
			for (JsonNode eventInfoNode : eventInfosNode) {
				JsonNode eventTimeframeNode = path(eventInfoNode, oldName);

				if (!eventTimeframeNode.isMissingNode()) {
					// If current node contains an "eventTimeframe", use the current node
					// to rename its child.
					renameField((ObjectNode)eventInfoNode, oldName, newName);
				}
			}
		}
	}

	private void removeTargetsFromEventsAndRemoveEventSuffix(JsonNode node) throws MigrationException {
		JsonNode eventInfosNode = path(node, "scenario/eventInfos");

		if (eventInfosNode.isArray()) {
			for (JsonNode eventInfoNode : eventInfosNode) {
				JsonNode eventsNode = path(eventInfoNode, "events");

				if (eventsNode.isArray()) {
					for (JsonNode currentEventNode : eventsNode) {
						remove(currentEventNode, "targets");

						JsonNode typeNode = currentEventNode.get("type");
						String removedEventSuffix = typeNode.asText().replace("Event", "");
						changeStringValue(currentEventNode, "type", removedEventSuffix);
					}
				}
			}
		}
	}

	private void renameEventsArrayToStimuli(JsonNode node) throws MigrationException {
		String oldName = "events";
		String newName = "stimuli";

		JsonNode eventInfosNode = path(node, "scenario/eventInfos");

		if (eventInfosNode.isArray()) {
			for (JsonNode eventInfoNode : eventInfosNode) {
				renameField((ObjectNode)eventInfoNode, oldName, newName);
			}
		}
	}

	private void renameEventInfosToStimulusInfos(JsonNode node) throws MigrationException {
		String oldName = "eventInfos";
		String newName = "stimulusInfos";

		JsonNode scenarioNode = path(node, "scenario");

		if (!scenarioNode.isMissingNode()) {
			JsonNode eventInfosNode = path(scenarioNode, oldName);

			if (!eventInfosNode.isMissingNode()) {
				renameField((ObjectNode)scenarioNode, oldName, newName);
			}
		}
	}

	private void renameMostImportantEventInDynamicElements(JsonNode node) throws MigrationException {
		String oldName = "mostImportantEvent";
		String newName = "mostImportantStimulus";

		JsonNode dynamicElementsNode = path(node, "scenario/topography/dynamicElements");

		if (dynamicElementsNode.isArray()) {
			for (JsonNode dynamicElementNode : dynamicElementsNode) {
				JsonNode mostImportantEventNode = path(dynamicElementNode, "mostImportantEvent");

				if (!mostImportantEventNode.isMissingNode()) {
					renameField((ObjectNode)dynamicElementNode, oldName, newName);
				}
			}
		}
	}

	private void renameUseSalientBehavior(JsonNode node) throws MigrationException {
		String oldName = "useSalientBehavior";
		String newName = "usePsychologyLayer";

		JsonNode attributesSimulationNode = path(node, "scenario/attributesSimulation");

		if (!attributesSimulationNode.isMissingNode()) {
			JsonNode useSalientBehaviorNode = path(attributesSimulationNode, oldName);

			if (!useSalientBehaviorNode.isMissingNode()) {
				renameField((ObjectNode)attributesSimulationNode, oldName, newName);
			}
		}
	}

	private void renameOutputProcessorMostImportantEvent(JsonNode node) throws MigrationException {
		String oldName = "FootStepMostImportantEventProcessor";
		String newName = "FootStepMostImportantStimulusProcessor";

		renameOutputProcessor(node, oldName, newName);
	}

	private void renameOutputProcessorSalientBehavior(JsonNode node) throws MigrationException {
		String oldName = "FootStepSalientBehaviorProcessor";
		String newName = "FootStepSelfCategoryProcessor";

		renameOutputProcessor(node, oldName, newName);
	}

	private void renameOutputProcessor(JsonNode node, String oldName, String newName) throws MigrationException {
		JsonNode processorsNode = path(node, "processWriters/processors");

		if (processorsNode.isArray()) {
			for (JsonNode processorNode : processorsNode) {
				String key = "type";
				String processorName = processorNode.get(key).asText("");

				if (processorName.contains(oldName)) {
					String newProcessorName = processorName.replace(oldName, newName);
					changeStringValue(processorNode, key, newProcessorName);
				}
			}
		}
	}

	private void renameSalientBehaviorInDynamicElements(JsonNode node) throws MigrationException {
		String oldName = "salientBehavior";
		String newName = "selfCategory";

		JsonNode dynamicElementsNode = path(node, "scenario/topography/dynamicElements");

		if (dynamicElementsNode.isArray()) {
			for (JsonNode dynamicElementNode : dynamicElementsNode) {
				JsonNode salientBehaviorNode = path(dynamicElementNode, "salientBehavior");

				if (!salientBehaviorNode.isMissingNode()) {
					renameField((ObjectNode)dynamicElementNode, oldName, newName);
				}
			}
		}
	}

	private void createNewPsychologyNodeInDynamicElements(JsonNode node) throws MigrationException {
		String newNodeName = "psychology";

		String[] nodeNamesToMove = new String[] {
			"mostImportantStimulus",
			"selfCategory"
		};

		JsonNode dynamicElementsNode = path(node, "scenario/topography/dynamicElements");

		if (dynamicElementsNode.isArray()) {
			for (JsonNode dynamicElementNode : dynamicElementsNode) {

				// Create parent node for the nodes which will be moved
				ObjectNode psychologyNode = JsonNodeFactory.instance.objectNode();

				for (String nodeName : nodeNamesToMove) {
					JsonNode nodeToMove = path(dynamicElementNode, nodeName);

					if (!nodeToMove.isMissingNode()) {
						psychologyNode.set(nodeName, nodeToMove);
						remove(dynamicElementNode, nodeName);
					}
				}

				// If new parent node is not empty, add it to the current "dynamicElement".
				// Otherwise, the "psychology" node will be added by the Jackson library
				// automatically.
				if (psychologyNode.elements().hasNext()) {
					((ObjectNode)dynamicElementNode).set(newNodeName, psychologyNode);
				}
			}
		}
	}

}
