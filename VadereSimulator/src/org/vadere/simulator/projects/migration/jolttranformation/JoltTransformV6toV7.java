package org.vadere.simulator.projects.migration.jolttranformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.util.StateJsonConverter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

@MigrationTransformation(targetVersionLabel = "0.7")
public class JoltTransformV6toV7 extends JoltTransformation{


	public JoltTransformV6toV7() {
		super(Version.V0_7);
	}

	@Override
	protected void initPostHooks() {
		postTransformHooks.add(this::setDefaultValues);
		postTransformHooks.add(this::renameProcessorAttribute);
		postTransformHooks.add(JoltTransformV1toV2::sort); // <-- allways last to ensure json order
	}

	// postHookStep
	private JsonNode setDefaultValues(JsonNode scenarioFile) throws MigrationException{
		JsonNode osmAttr = path(scenarioFile, "scenario/attributesModel/org.vadere.state.attributes.models.AttributesOSM");
		AttributesOSM attributesOSM = new AttributesOSM();
		if (!osmAttr.isMissingNode()){
			double stepLengthIntercept = pathMustExist(osmAttr, "stepLengthIntercept").doubleValue();

			if(!osmAttr.has("minStepLength")){
				addToObjectNode(osmAttr, "minStepLength", Double.toString(attributesOSM.getMinStepLength()));
			}

			if(!osmAttr.has("maxStepDuration")){
				addToObjectNode(osmAttr, "maxStepDuration", Double.toString(attributesOSM.getMaxStepDuration()));
			}

		}
		return scenarioFile;
	}

	private JsonNode renameProcessorAttribute(JsonNode scenarioFile) throws MigrationException {
		ArrayList<JsonNode> gaussianProcessor =
				getProcessorsByType(scenarioFile, "org.vadere.simulator.projects.dataprocessing.processor.PedestrianDensityGaussianProcessor");
		for (JsonNode p : gaussianProcessor) {
			JsonNode attr = pathMustExist(p, "attributes");
			renameField((ObjectNode)attr, "standardDerivation", "standardDeviation");
		}

		return scenarioFile;
	}

	private JsonNode removeShapeFromDataProcessors(JsonNode scenarioFile, ObjectMapper mapper) throws MigrationException, IOException {
		ArrayList<JsonNode> processor =
				getProcessorsByType(scenarioFile, "org.vadere.simulator.projects.dataprocessing.processor.FundamentalDiagramBProcessor");
		for (JsonNode p : processor) {
			JsonNode attr = pathMustExist(p, "attributes");
			JsonNode measurementArea = path(attr, "measurementArea");
			if (!measurementArea.isMissingNode()){
				ArrayList<MeasurementArea> measurementAreas = deserializeMeasurementArea(scenarioFile, mapper);
				//todo add new area, test existing.
				System.out.println(measurementArea.toString());
			}
		}

		return scenarioFile;
	}

	public static void main(String[] arg) throws Exception {
		BufferedReader r = new BufferedReader(
				new FileReader("/home/lphex/hm.d/vadere/VadereModelTests/TestOSM/scenarios/rimea_04_flow_osm1_125_h.scenario"));
		String jsonStr = r.lines().collect(Collectors.joining("\n"));
		ObjectMapper mapper = StateJsonConverter.getMapper();
		JsonNode jsonNode = StateJsonConverter.deserializeToNode(jsonStr);
		JoltTransformV6toV7 j = new JoltTransformV6toV7();
		j.removeShapeFromDataProcessors(jsonNode, mapper);

	}

}
