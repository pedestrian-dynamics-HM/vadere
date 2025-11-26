package org.vadere.simulator.projects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles.ATriangleMeshWithDataStorage;
import org.vadere.state.scenario.Topography;

public class Domain {
	private final @Nullable ATriangleMeshWithDataStorage floorFieldMesh;
    private final @Nullable ATriangleMeshWithDataStorage backgroundMesh;
	private final @NotNull Topography topography;

	public Domain(@NotNull final Topography topography) {
		this.floorFieldMesh = null;
		this.backgroundMesh = null;
		this.topography = topography;
	}

	public Domain(@Nullable final ATriangleMeshWithDataStorage floorFieldMesh, @NotNull final Topography topography) {
		this.floorFieldMesh = floorFieldMesh;
        this.backgroundMesh = null;
		this.topography = topography;
	}

	public Domain(@Nullable final ATriangleMeshWithDataStorage floorFieldMesh, @Nullable final ATriangleMeshWithDataStorage backgroundMesh, @NotNull final Topography topography) {
		this.floorFieldMesh = floorFieldMesh;
		this.backgroundMesh = backgroundMesh;
		this.topography = topography;
	}

	@Nullable
	public ATriangleMeshWithDataStorage getFloorFieldMesh() {
		return floorFieldMesh;
	}

	@Nullable
	public ATriangleMeshWithDataStorage getBackgroundMesh() {
		return backgroundMesh;
	}

	public Topography getTopography() {
		return topography;
	}

	public Domain clone() {
		return new Domain(floorFieldMesh == null ? null : (ATriangleMeshWithDataStorage) floorFieldMesh.clone(), backgroundMesh == null ? null : (ATriangleMeshWithDataStorage) backgroundMesh.clone(), topography.clone());
	}
}
