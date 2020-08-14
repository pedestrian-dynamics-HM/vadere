package org.vadere.simulator.projects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.state.scenario.Topography;

public class Domain {
	private final @Nullable AMesh floorFieldMesh;
	private final @Nullable AMesh backgroundMesh;
	private final @NotNull Topography topography;

	public Domain(@NotNull final Topography topography) {
		this.floorFieldMesh = null;
		this.backgroundMesh = null;
		this.topography = topography;
	}

	public Domain(@Nullable final AMesh floorFieldMesh, @NotNull final Topography topography) {
		this.floorFieldMesh = floorFieldMesh;
		this.backgroundMesh = null;
		this.topography = topography;
	}

	public Domain(@Nullable final AMesh floorFieldMesh, @Nullable final AMesh backgroundMesh, @NotNull final Topography topography) {
		this.floorFieldMesh = floorFieldMesh;
		this.backgroundMesh = backgroundMesh;
		this.topography = topography;
	}

	@Nullable
	public AMesh getFloorFieldMesh() {
		return floorFieldMesh;
	}

	@Nullable
	public AMesh getBackgroundMesh() {
		return backgroundMesh;
	}

	public Topography getTopography() {
		return topography;
	}

	public Domain clone() {
		return new Domain(floorFieldMesh == null ? null : floorFieldMesh.clone(), backgroundMesh == null ? null : backgroundMesh.clone(), topography.clone());
	}
}
