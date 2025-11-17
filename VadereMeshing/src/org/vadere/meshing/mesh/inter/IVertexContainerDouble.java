package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

public interface IVertexContainerDouble<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	double getValue(@NotNull final V vertex);

	void setValue(@NotNull final V vertex, double value);

	void reset();
}
