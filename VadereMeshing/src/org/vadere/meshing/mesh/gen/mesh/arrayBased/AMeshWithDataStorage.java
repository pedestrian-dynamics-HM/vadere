package org.vadere.meshing.mesh.gen.mesh.arrayBased;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.*;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

public class AMeshWithDataStorage implements IMeshWithDataStorage<AVertex, AHalfEdge, AFace> {
    private final IMesh<AVertex, AHalfEdge, AFace> mesh;
    private final IMeshDataStorage<AVertex, AHalfEdge, AFace> dataStorage;

    public AMeshWithDataStorage(IMesh<AVertex, AHalfEdge, AFace> mesh, IMeshDataStorage<AVertex, AHalfEdge, AFace> dataStorage) {
        this.mesh = mesh;
        this.dataStorage = dataStorage;
    }

    @Override
    public IMesh<AVertex, AHalfEdge, AFace> getMesh() {
        return mesh;
    }

    @Override
    public IMeshDataStorage<AVertex, AHalfEdge, AFace> getDataStorage() {
        return dataStorage;
    }

    @Override
    public IMeshWithDataStorage<AVertex, AHalfEdge, AFace> clone() {
        IMesh<AVertex, AHalfEdge, AFace> clone = mesh.clone();
        return new AMeshWithDataStorage(clone, dataStorage.clone(clone));
    }

    @Override
    public IMeshBuilder<AVertex, AHalfEdge, AFace> toMutableMesh() {
        return new AMeshBuilder(this);
    }

    public IMeshWithDataStorage<AVertex, AHalfEdge, AFace> toNewEmptyMeshWithDataStorage(){
        IMesh<AVertex, AHalfEdge, AFace> emptyMesh = mesh.constructEmpty();
        IMeshDataStorage<AVertex, AHalfEdge, AFace> emptyDataStorage = mesh.createEmptyDataStorage();
        return new AMeshWithDataStorage(emptyMesh, emptyDataStorage);
    }

    @Override
    public void clear() {
        mesh.clear();
        dataStorage.clear();
    }

    public static IMeshWithDataStorage<AVertex, AHalfEdge, AFace> constructEmpty(){
        AMesh emptyMesh = new AMesh();
        IMeshDataStorage<AVertex, AHalfEdge, AFace> emptyDataStorage = emptyMesh.createEmptyDataStorage();
        emptyMesh.meshDataStorage = (AMeshDataStorage) emptyDataStorage; // todo hh: rework when removing data storage from mesh
        return new AMeshWithDataStorage(emptyMesh, emptyDataStorage);
    }

    @Override
    public IIncrementalTriangulation<AVertex, AHalfEdge, AFace> toTriangulation(final @NotNull IPointLocator.Type type) {
        return IIncrementalTriangulation.createATriangulation(type, this);
    }

    @Override
    public IIncrementalTriangulation<AVertex, AHalfEdge, AFace> toTriangulation() {
        return IIncrementalTriangulation.createATriangulation(IPointLocator.Type.JUMP_AND_WALK, this);
    }
}
