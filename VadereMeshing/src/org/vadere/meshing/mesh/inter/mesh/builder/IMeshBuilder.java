package org.vadere.meshing.mesh.inter.mesh.builder;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

public interface IMeshBuilder<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    IMeshOptimizer<V, E, F> getOptimizer();
    IMeshWithDataStorage<V, E, F> build();
}
