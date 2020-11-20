package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;

public interface IEdgeContainerDouble <V extends IVertex, E extends IHalfEdge, F extends IFace> {

	double getValue(@NotNull final E edge);

	void setValue(@NotNull final E edge, double value);
}
