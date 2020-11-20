package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;

public interface IVertexContainerObject<V extends IVertex, E extends IHalfEdge, F extends IFace, O> {

	O getValue(@NotNull final V vertex);

	void setValue(@NotNull final V vertex, O value);
}
