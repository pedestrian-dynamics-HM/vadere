package org.vadere.meshing.mesh.inter.mesh.builder;

import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;
import org.vadere.meshing.mesh.inter.meshConnectivity.ITriConnectivity;

public interface ITriangleMeshBuilder<V extends IVertex, E extends IHalfEdge, F extends IFace>
        extends IMeshBuilder<V, E, F> {
    ITriangleMesh<V, E, F> getMesh();
    IMeshDataStorage<V, E, F> getDataStorage();
    ITriangleMeshBuilder<V, E, F> copy();
    ITriangleMeshBuilder<V, E, F> newInstance();
    ITriangleMeshWithDataStorage<V, E, F> getMeshWithDataStorage();

    ITriConnectivity<V, E, F> changeConnectivity();
}
