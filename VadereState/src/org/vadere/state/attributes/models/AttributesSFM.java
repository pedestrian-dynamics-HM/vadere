package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.types.GradientProviderType;

@ModelAttributeClass
public class AttributesSFM extends Attributes {
	private final AttributesODEIntegrator attributesODEIntegrator;
	private final GradientProviderType floorGradientProviderType = GradientProviderType.FLOOR_EIKONAL_DISCRETE;

	private final String targetPotentialModel = "org.vadere.simulator.models.potential.fields.PotentialFieldTargetGrid";
	private final String pedestrianPotentialModel = "org.vadere.simulator.models.sfm.PotentialFieldPedestrianSFM";
	private final String obstaclePotentialModel = "org.vadere.simulator.models.sfm.PotentialFieldObstacleSFM";

	public AttributesSFM() {
		attributesODEIntegrator = new AttributesODEIntegrator();
	}

	// Getter...
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
