package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;

public interface IVertexContainerInteger<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	int getValue(@NotNull final V vertex);

	void setValue(@NotNull final V vertex, int value);
}

