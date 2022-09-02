package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.util.Attributes;
import org.vadere.state.types.GradientProviderType;

@ModelAttributeClass
public class AttributesGNM extends Attributes {

	private AttributesODEIntegrator attributesODEIntegrator;
	private GradientProviderType floorGradientProviderType = GradientProviderType.FLOOR_EIKONAL_DISCRETE;

	private String targetPotentialModel = "org.vadere.simulator.models.potential.fields.PotentialFieldTargetGrid";
	private String pedestrianPotentialModel = "org.vadere.simulator.models.gnm.PotentialFieldPedestrianGNM";
	private String obstaclePotentialModel = "org.vadere.simulator.models.gnm.PotentialFieldObstacleGNM";

	public AttributesGNM() {
		attributesODEIntegrator = new AttributesODEIntegrator();
	}

	// Getters...
	public AttributesODEIntegrator getAttributesODEIntegrator() {
		return attributesODEIntegrator;
	}

	public GradientProviderType getFloorGradientProviderType() {
		return floorGradientProviderType;
	}

	public String getTargetPotentialModel() {
		return targetPotentialModel;
	}

	public String getPedestrianPotentialModel() {
		return pedestrianPotentialModel;
	}

	public String getObstaclePotentialModel() {
		return obstaclePotentialModel;
	}
}
