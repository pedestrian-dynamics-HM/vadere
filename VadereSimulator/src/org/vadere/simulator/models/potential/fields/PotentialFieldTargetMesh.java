package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.List;
import java.util.Random;

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
