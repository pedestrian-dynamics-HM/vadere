package org.vadere.simulator.projects.migration.jsontranformation.jolt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.util.version.Version;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.JoltTransformation;
import org.vadere.state.attributes.models.AttributesOSM;

import java.util.ArrayList;

@MigrationTransformation(targetVersionLabel = "0.7")
public class JoltTransformV6toV7 extends JoltTransformation {


	public JoltTransformV6toV7() {
		super(Version.V0_7);
	}

	@Override
	protected void initDefaultHooks() {
        addPostHookLast(this::setDefaultValues);
        addPostHookLast(this::renameProcessorAttribute);
		addPostHookLast(this::sort);
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

}
