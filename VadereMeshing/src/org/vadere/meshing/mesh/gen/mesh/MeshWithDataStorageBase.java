package org.vadere.meshing.mesh.gen.mesh;

import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

public abstract class MeshWithDataStorageBase<V extends IVertex, E extends IHalfEdge, F extends IFace, Mesh extends IMesh<V, E, F>> implements IMeshWithDataStorage<V, E, F> {
    private final Mesh mesh;
    private final IMeshDataStorage<V, E, F> dataStorage;

    public MeshWithDataStorageBase(Mesh mesh, IMeshDataStorage<V, E, F> dataStorage) {
        this.mesh = mesh;
        this.dataStorage = dataStorage;
    }

    @Override
    public Mesh getMesh() {
        return mesh;
    }

    @Override
    public IMeshDataStorage<V, E, F> getDataStorage() {
        return dataStorage;
    }

    @Override
    public abstract IMeshWithDataStorage<V, E, F> clone();
    @Override
    public abstract IMeshBuilder<V, E, F> modify();
}
