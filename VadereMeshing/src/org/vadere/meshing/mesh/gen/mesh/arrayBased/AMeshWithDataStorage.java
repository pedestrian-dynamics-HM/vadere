package org.vadere.meshing.mesh.gen.mesh.arrayBased;

import org.vadere.meshing.mesh.gen.mesh.MeshWithDataStorageBase;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.*;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

public class AMeshWithDataStorage extends MeshWithDataStorageBase<AVertex, AHalfEdge, AFace, AMesh> {

    public AMeshWithDataStorage(AMesh mesh, IMeshDataStorage<AVertex, AHalfEdge, AFace> dataStorage) {
        super(mesh, dataStorage);
    }

    @Override
    public IMeshWithDataStorage<AVertex, AHalfEdge, AFace> clone() {
        AMesh meshClone = getMesh().copy();
        return new AMeshWithDataStorage(meshClone, getDataStorage().clone(meshClone));
    }

    @Override
    public IMeshBuilder<AVertex, AHalfEdge, AFace> modify() {
        return new AMeshBuilder(this);
    }
}
