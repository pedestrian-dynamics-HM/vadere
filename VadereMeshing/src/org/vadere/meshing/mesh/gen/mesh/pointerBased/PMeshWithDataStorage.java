package org.vadere.meshing.mesh.gen.mesh.pointerBased;

import org.vadere.meshing.mesh.gen.mesh.MeshWithDataStorageBase;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.*;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

public class PMeshWithDataStorage extends MeshWithDataStorageBase<PVertex, PHalfEdge, PFace, PMesh> {

    public PMeshWithDataStorage(PMesh mesh, IMeshDataStorage<PVertex, PHalfEdge, PFace> dataStorage) {
        super(mesh, dataStorage);
    }

    @Override
    public IMeshWithDataStorage<PVertex, PHalfEdge, PFace> clone() {
        PMesh meshClone = getMesh().copy();
        return new PMeshWithDataStorage(meshClone, getDataStorage().clone(meshClone));
    }

    @Override
    public IMeshBuilder<PVertex, PHalfEdge, PFace> modify() {
        return new PMeshBuilder(this);
    }
}
