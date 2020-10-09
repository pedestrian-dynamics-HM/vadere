package org.vadere.simulator.projects.migration.jsontranformation.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.dataprocessing.processor.PedestrianEndTimeProcessor;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.state.attributes.AttributesPsychology;
import org.vadere.util.version.Version;

import java.util.ArrayList;
import java.util.Iterator;

@MigrationTransformation(targetVersionLabel =  "1.15")
public class TargetVersionV1_15 extends SimpleJsonTransformation {

	public TargetVersionV1_15() {
		super(Version.V1_15);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookFirst(this::changeEvacTimeProc);
		addPostHookLast(this::sort);
	}

	private JsonNode changeEvacTimeProc(JsonNode node) throws MigrationException {
		Iterator<JsonNode> iter = iteratorProcessorsByType(node,"org.vadere.simulator.projects.dataprocessing.processor.EvacuationTimeProcessor");

		if (iter.hasNext()) { // if evacuationtimePRocessor(s) are present

			// find PedestrianStartTimeProcessor
			Iterator<JsonNode> iter_start_proc = iteratorProcessorsByType(node,"org.vadere.simulator.projects.dataprocessing.processor.PedestrianStartTimeProcessor");
			int startTimeProcId = -1;
			if(iter_start_proc.hasNext()){
				JsonNode processort_start = iter_start_proc.next();
				startTimeProcId = processort_start.get("id").intValue();
			}else{
				// create new - should not happen!
				startTimeProcId= getFreeId(node);
				ArrayNode processorNode = (ArrayNode)pathMustExist(node, "processWriters/processors");
				ObjectNode newProcessor = processorNode.addObject();
				newProcessor.put("type", "org.vadere.simulator.projects.dataprocessing.processor.PedestrianStartTimeProcessor");
				newProcessor.put("id", startTimeProcId);
			}

			// PedestrianEndTimeProcessor
			Iterator<JsonNode> iter_end_proc = iteratorProcessorsByType(node,"org.vadere.simulator.projects.dataprocessing.processor.PedestrianEndTimeProcessor");
			int endTimeProcId = -1;
			if(iter_start_proc.hasNext()){
				JsonNode processor_end = iter_end_proc.next();
				endTimeProcId = processor_end.get("id").intValue();
			}else{
				// create new
				endTimeProcId= getFreeId(node);
				ArrayNode processorNode = (ArrayNode)pathMustExist(node, "processWriters/processors");
				ObjectNode newProcessor = processorNode.addObject();
				newProcessor.put("type", "org.vadere.simulator.projects.dataprocessing.processor.PedestrianEndTimeProcessor");
				newProcessor.put("id", endTimeProcId);
			}

			// adapt EvacTimeProc - updated iterator
			Iterator<JsonNode> iter_evac_proc = iteratorProcessorsByType(node,"org.vadere.simulator.projects.dataprocessing.processor.PedestrianEvacuationTimeProcessor");
			while(iter_evac_proc.hasNext()){
				JsonNode evacProc = iter_evac_proc.next();
				//remove(processor, "attributes"); // remove existing attributes, add new set of attributes
				ObjectNode attr = (ObjectNode) evacProc.get("attributes");
				attr.put("org.vadere.simulator.projects.dataprocessing.processor.PedestrianStartTimeProcessor", startTimeProcId);
				attr.put("org.vadere.simulator.projects.dataprocessing.processor.PedestrianEndTimeProcessor", endTimeProcId);
				attr.remove("org.vadere.simulator.projects.dataprocessing.processor.PedestrianEvcuationTimeProcessor");
			}
		}
		return node;
	}

	private JsonNode addDependentProc(JsonNode node) throws MigrationException {
		ArrayNode processors = (ArrayNode)pathMustExist(node, "processWriters/processors");
		ObjectNode newProcessor = processors.addObject();
		newProcessor.put("type", "org.vadere.simulator.projects.dataprocessing.processor.PedestrianEndTimeProcessor");
		newProcessor.put("id", getFreeId(node));
		Iterator<JsonNode> iter = iteratorProcessorsByType(node,"org.vadere.simulator.projects.dataprocessing.processor.PedestrianStartTimeProcessor");
		// check that there is only one?
		while (iter.hasNext()) {
			JsonNode processor = iter.next();
			// to something with PedestrianStartTimeProcessor
			Integer id = processor.get("id").asInt();  //???
		}
		return node;
	}


}
