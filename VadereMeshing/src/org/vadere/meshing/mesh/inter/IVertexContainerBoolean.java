package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;

public interface IVertexContainerBoolean<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	boolean getValue(@NotNull final V vertex);

	void setValue(@NotNull final V vertex, boolean value);
}

