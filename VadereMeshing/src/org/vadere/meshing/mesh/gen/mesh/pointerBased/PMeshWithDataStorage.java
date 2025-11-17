package org.vadere.meshing.mesh.gen.mesh.pointerBased;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

public class PMeshWithDataStorage implements IMeshWithDataStorage<PVertex, PHalfEdge, PFace> {
    private final IMesh<PVertex, PHalfEdge, PFace> mesh;
    private final IMeshDataStorage<PVertex, PHalfEdge, PFace> dataStorage;

    public PMeshWithDataStorage(IMesh<PVertex, PHalfEdge, PFace> mesh, IMeshDataStorage<PVertex, PHalfEdge, PFace> dataStorage) {
        this.mesh = mesh;
        this.dataStorage = dataStorage;
    }

    @Override
    public IMesh<PVertex, PHalfEdge, PFace> getMesh() {
        return mesh;
    }

    @Override
    public IMeshDataStorage<PVertex, PHalfEdge, PFace> getDataStorage() {
        return dataStorage;
    }

    @Override
    public IMeshWithDataStorage<PVertex, PHalfEdge, PFace> clone() {
        IMesh<PVertex, PHalfEdge, PFace> clone = mesh.clone();
        return new PMeshWithDataStorage(clone, dataStorage.clone(clone));
    }

    @Override
    public IMeshBuilder<PVertex, PHalfEdge, PFace> toMutableMesh() {
        return new PMeshBuilder(this);
    }

    public IMeshWithDataStorage<PVertex, PHalfEdge, PFace> toNewEmptyMeshWithDataStorage(){
        IMesh<PVertex, PHalfEdge, PFace> emptyMesh = mesh.constructEmpty();
        IMeshDataStorage<PVertex, PHalfEdge, PFace> emptyDataStorage = mesh.createEmptyDataStorage();
        return new PMeshWithDataStorage(emptyMesh, emptyDataStorage);
    }

    public static IMeshWithDataStorage<PVertex, PHalfEdge, PFace> constructEmpty(){
        IMesh<PVertex, PHalfEdge, PFace> emptyMesh = new PMesh();
        IMeshDataStorage<PVertex, PHalfEdge, PFace> emptyDataStorage = emptyMesh.createEmptyDataStorage();
        return new PMeshWithDataStorage(emptyMesh, emptyDataStorage);
    }

    @Override
    public IIncrementalTriangulation<PVertex, PHalfEdge, PFace> toTriangulation(@NotNull final IPointLocator.Type type) {
        return IIncrementalTriangulation.createTriangulation(type, this);
    }

    @Override
    public IIncrementalTriangulation<PVertex, PHalfEdge, PFace> toTriangulation() {
        return IIncrementalTriangulation.createTriangulation(IPointLocator.Type.JUMP_AND_WALK, this);
    }


    @Override
    public void clear() {
        mesh.clear();
        dataStorage.clear();
    }
}
