package org.vadere.meshing.mesh.gen.mesh;

import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.mesh.builder.*;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

public abstract class MeshBuilderBase<V extends IVertex, E extends IHalfEdge, F extends IFace,
        DataStorage extends IMeshDataStorage<V, E, F>,
        Mesh extends IMesh<V, E, F>
        > implements IMeshBuilder<V, E, F> {
    DataStorage meshDataStorage;
    Mesh mesh;

    public MeshBuilderBase(IMeshWithDataStorage<V, E, F> toEdit) {
        meshDataStorage = (DataStorage) toEdit.getDataStorage();
        mesh = (Mesh) toEdit.getMesh();
    }

    public MeshBuilderBase(Mesh mesh, DataStorage dataStorage) {
        this.mesh = mesh;
        meshDataStorage = dataStorage;
    }

    @Override
    public Mesh getMesh() {
        return mesh;
    }

    @Override
    public DataStorage getDataStorage() {
        return meshDataStorage;
    }

    @Override
    public abstract IMeshBuilderEdges<V, E, F> edges();

    @Override
    public abstract IMeshBuilderVertices<V, E, F> vertices();

    @Override
    public abstract IMeshBuilderFaces<V, E, F> faces();

    public abstract IMeshOptimizer<V, E, F> getOptimizer();

    public abstract IMeshWithDataStorage<V, E, F> getMeshWithDataStorage();
}
