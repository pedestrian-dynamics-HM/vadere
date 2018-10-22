package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.geometry.Vector2D;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VShape;

/**
 * Created by bzoennchen on 05.03.18.
 */
public class PotentialFieldTargetMesh extends PotentialFieldTarget {


	public PotentialFieldTargetMesh(@NotNull final Topography topography,
	                                @NotNull final AttributesAgent attributesPedestrian,
	                                @NotNull final AttributesFloorField attributesPotential) {
		super(topography, attributesPedestrian, attributesPotential);
	}
}
