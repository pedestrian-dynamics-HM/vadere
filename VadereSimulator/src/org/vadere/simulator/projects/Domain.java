package org.vadere.simulator.projects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.gen.PMesh;
import org.vadere.state.scenario.Topography;

public class Domain {
	private final @Nullable PMesh backgroundMesh;
	private final @NotNull Topography topography;

	public Domain(@NotNull final Topography topography) {
		this.backgroundMesh = null;
		this.topography = topography;
	}

	public Domain(@Nullable final PMesh backgroundMesh, @NotNull final Topography topography) {
		this.backgroundMesh = backgroundMesh;
		this.topography = topography;
	}

	public PMesh getBackgroundMesh() {
		return backgroundMesh;
	}

	public Topography getTopography() {
		return topography;
	}
	
}
