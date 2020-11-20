package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;

public interface IVertexContainerDouble<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	double getValue(@NotNull final V vertex);

	void setValue(@NotNull final V vertex, double value);

	void reset();
}
