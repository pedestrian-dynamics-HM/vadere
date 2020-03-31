package org.vadere.simulator.projects.migration.jsontranformation.jolt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.JoltTransformation;

import java.util.Iterator;

@MigrationTransformation(targetVersionLabel = "0.6")
public class JoltTransformV5toV6 extends JoltTransformation {

	public JoltTransformV5toV6() {
		super(Version.V0_6);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookLast(this::removePedestrianOverlapDistProcessor);
		addPostHookLast(this::changePedestrianMaxOverlapProcessorAttributes);
		addPostHookLast(this::sort);
	}

	private JsonNode removePedestrianOverlapDistProcessor(JsonNode node) throws MigrationException {
		ArrayNode processors = (ArrayNode)pathMustExist(node, "processWriters/processors");
		Iterator<JsonNode> iter = processors.iterator();
		while (iter.hasNext()){
			JsonNode p = iter.next();
			String type = pathMustExist(p, "type").asText();
			if (type.equals("org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapDistProcessor")){
				String processorId = pathMustExist(p, "id").asText();
				Iterator<JsonNode> fileIter = iteratorFilesForProcessorId(node, processorId);
				while (fileIter.hasNext()){
					JsonNode file = fileIter.next();
					arrayRemoveString((ArrayNode)pathMustExist(file, "processors"), processorId);
				}
				iter.remove();
			}
		}
		return node;
	}

	private JsonNode changePedestrianMaxOverlapProcessorAttributes(JsonNode node) throws MigrationException {
		Iterator<JsonNode> processIter = iteratorProcessorsByType(node,"org.vadere.simulator.projects.dataprocessing.processor.PedestrianOverlapProcessor");
		while (processIter.hasNext()) {
			JsonNode processor = processIter.next();
			String pId = pathMustExist(processor, "id").asText();

			// check all files the processor writes to for the correct Key.
			Iterator<JsonNode> fileIter = iteratorFilesForProcessorId(node, pId);
			boolean wrongFile = false;
			while (fileIter.hasNext()){
				JsonNode file = fileIter.next();
				String fileType = pathMustExist(file, "type").asText();
				if (!fileType.equals("org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOverlapOutputFile")){
					arrayRemoveString((ArrayNode)pathMustExist(file, "processors"), pId);
					wrongFile = true;
				}
			}

			// if wrong mapping occurred (wrongFile == true) create a new outputFile an place the
			// the pId in it.
			if (wrongFile){
				createNewOutputFile(node, "org.vadere.simulator.projects.dataprocessing.outputfile.TimestepPedestrianIdOverlapOutputFile", pId);
			}
		}
		return node;

	}
}
