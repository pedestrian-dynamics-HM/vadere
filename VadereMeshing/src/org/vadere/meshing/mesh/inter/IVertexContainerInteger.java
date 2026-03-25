package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

public interface IVertexContainerInteger<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	int getValue(@NotNull final V vertex);

	void setValue(@NotNull final V vertex, int value);
}

