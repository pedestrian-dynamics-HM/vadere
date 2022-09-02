package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.util.Attributes;
import org.vadere.state.types.GradientProviderType;

@ModelAttributeClass
public class AttributesParticles extends Attributes {
	private AttributesODEIntegrator attributesODEIntegrator;
	private GradientProviderType floorGradientProviderType = GradientProviderType.FLOOR_EIKONAL_DISCRETE;

	public AttributesParticles() {
		attributesODEIntegrator = new AttributesODEIntegrator();
	}

	// Getter...
	public AttributesODEIntegrator getAttributesODEIntegrator() {
		return attributesODEIntegrator;
	}

	public GradientProviderType getFloorGradientProviderType() {
		return floorGradientProviderType;
	}
}
