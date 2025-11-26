package org.vadere.meshing.mesh.inter.mesh;

import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

public interface IMeshWithDataStorage<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    IMesh<V, E, F> getMesh();
    IMeshDataStorage<V, E, F> getDataStorage();

    IMeshWithDataStorage<V, E, F> clone();

    /**
     * This allows modification of the mesh after creation. Use with care.
     */
    IMeshBuilder<V, E, F> modify();
}
