package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;

public interface IEdgeContainerObject<V extends IVertex, E extends IHalfEdge, F extends IFace, O> {

	O getValue(@NotNull final E edge);

	void setValue(@NotNull final E edge, O value);
}
