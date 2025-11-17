package org.vadere.simulator.projects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.AMesh;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.AMeshDataStorage;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.AMeshWithDataStorage;
import org.vadere.state.scenario.Topography;

public class Domain {
	private final @Nullable AMeshWithDataStorage floorFieldMesh;
    private final @Nullable AMeshWithDataStorage backgroundMesh;
	private final @NotNull Topography topography;

	public Domain(@NotNull final Topography topography) {
		this.floorFieldMesh = null;
		this.backgroundMesh = null;
		this.topography = topography;
	}

	public Domain(@Nullable final AMeshWithDataStorage floorFieldMesh, @NotNull final Topography topography) {
		this.floorFieldMesh = floorFieldMesh;
        this.backgroundMesh = null;
		this.topography = topography;
	}

	public Domain(@Nullable final AMeshWithDataStorage floorFieldMesh, @Nullable final AMeshWithDataStorage backgroundMesh, @NotNull final Topography topography) {
		this.floorFieldMesh = floorFieldMesh;
		this.backgroundMesh = backgroundMesh;
		this.topography = topography;
	}

	@Nullable
	public AMeshWithDataStorage getFloorFieldMesh() {
		return floorFieldMesh;
	}

	@Nullable
	public AMeshWithDataStorage getBackgroundMesh() {
		return backgroundMesh;
	}

	public Topography getTopography() {
		return topography;
	}

	public Domain clone() {
		return new Domain(floorFieldMesh == null ? null : (AMeshWithDataStorage) floorFieldMesh.clone(), backgroundMesh == null ? null : (AMeshWithDataStorage) backgroundMesh.clone(), topography.clone());
	}
}
