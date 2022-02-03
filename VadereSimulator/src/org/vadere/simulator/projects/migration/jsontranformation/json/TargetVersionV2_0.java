package org.vadere.simulator.projects.migration.jsontranformation.json;

import java.util.ArrayList;

import org.vadere.annotation.factories.migrationassistant.MigrationTransformation;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.jsontranformation.SimpleJsonTransformation;
import org.vadere.util.version.Version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@MigrationTransformation(targetVersionLabel = "2.0")
public class TargetVersionV2_0 extends SimpleJsonTransformation {

	private final String fieldDistributionName = "interSpawnTimeDistribution";
	private final String fieldDistributionParametersName = "distributionParameters";

	public TargetVersionV2_0() {
		super(Version.V2_0);
	}

	@Override
	protected void initDefaultHooks() {
		addPostHookLast(this::alterDistributionDefinition);
		addPostHookLast(this::sort);

	}

	private JsonNode alterDistributionDefinition(JsonNode scenarioFile) throws MigrationException {
		JsonNode sources = path(scenarioFile, "scenario/topography/sources");

		if (sources.isArray()) {
			for (JsonNode source : sources) {
				String distributionName = source.get(fieldDistributionName).asText();
				
				switch (distributionName) {
				case "org.vadere.state.scenario.ConstantDistribution":
					alterConstantDistributionDefinition(source);
					break;

				case "org.vadere.state.scenario.NegativeExponentialDistribution":
					alterNegativeExponentialDistributionDefinition(source);
					break;

				case "org.vadere.state.scenario.PoissonDistribution":
					alterPoissonDistributionDefinition(source);
					break;

				default:
					throw new MigrationException("No action specified for distribution: " + distributionName);
				}
			}
		}

		return scenarioFile;
	}

	private void alterConstantDistributionDefinition(JsonNode source) throws MigrationException {
		changeStringValue(source, fieldDistributionName, "constant");
		ArrayList<Double> parametersOld = getDoubleList(source, fieldDistributionParametersName);
		double updateFrequency = parametersOld.get(0);

		ObjectNode objNode = (ObjectNode) source;

		ObjectNode parameters = objNode.putObject(fieldDistributionParametersName);
		addDoubleField(parameters, "updateFrequency", updateFrequency);
	}

	private void alterNegativeExponentialDistributionDefinition(JsonNode source) throws MigrationException {
		changeStringValue(source, fieldDistributionName, "negativeExponential");
		ArrayList<Double> parametersOld = getDoubleList(source, fieldDistributionParametersName);
		double mean = parametersOld.get(0);

		ObjectNode objNode = (ObjectNode) source;

		ObjectNode parameters = objNode.putObject(fieldDistributionParametersName);
		addDoubleField(parameters, "mean", mean);

	}

	private void alterPoissonDistributionDefinition(JsonNode source) throws MigrationException {
		changeStringValue(source, fieldDistributionName, "poisson");
		ArrayList<Double> parametersOld = getDoubleList(source, fieldDistributionParametersName);
		double numberPedsPerSecond = parametersOld.get(0);

		ObjectNode objNode = (ObjectNode) source;

		ObjectNode parameters = objNode.putObject(fieldDistributionParametersName);
		addDoubleField(parameters, "numberPedsPerSecond", numberPedsPerSecond);
	}

}
