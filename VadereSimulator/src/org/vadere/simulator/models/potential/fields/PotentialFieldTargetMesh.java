package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;

import java.nio.file.Path;

/**
 * Created by bzoennchen on 05.03.18.
 */
public class PotentialFieldTargetMesh extends PotentialFieldTarget {


	public PotentialFieldTargetMesh(@NotNull final Topography topography,
	                                @NotNull final AttributesAgent attributesPedestrian,
	                                @NotNull final AttributesFloorField attributesPotential,
									@NotNull final Path cacheDir) {
		super(topography, attributesPedestrian, attributesPotential, cacheDir);
	}
}
