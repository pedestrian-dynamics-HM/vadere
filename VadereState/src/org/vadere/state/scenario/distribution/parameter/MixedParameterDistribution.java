package org.vadere.state.scenario.distribution.parameter;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Aleksandar Ivanov
 */

public class MixedParameterDistribution {
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