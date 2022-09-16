package org.vadere.state.scenario.distribution.parameter;

import com.fasterxml.jackson.databind.JsonNode;
import org.vadere.state.attributes.Attributes;

/**
 * @author Aleksandar Ivanov
 */

public class MixedParameterDistribution extends Attributes {
	private String interSpawnTimeDistribution;
	private JsonNode distributionParameters;

	public String getInterSpawnTimeDistribution() {
		return interSpawnTimeDistribution;
	}

	public void setInterSpawnTimeDistribution(String interSpawnTimeDistribution) {
		this.interSpawnTimeDistribution = interSpawnTimeDistribution;
	}

	public JsonNode getDistributionParameters() {
		return distributionParameters;
	}

	public void setDistributionParameters(JsonNode distributionParameters) {
		this.distributionParameters = distributionParameters;
	}
}