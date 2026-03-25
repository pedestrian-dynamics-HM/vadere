package org.vadere.meshing.mesh.inter;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

public interface IEdgeContainerBoolean<V extends IVertex, E extends IHalfEdge, F extends IFace> {

	boolean getValue(@NotNull final E edge);

	void setValue(@NotNull final E edge, boolean value);
}
