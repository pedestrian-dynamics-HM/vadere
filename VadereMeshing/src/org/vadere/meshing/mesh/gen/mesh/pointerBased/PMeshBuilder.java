package org.vadere.meshing.mesh.gen.mesh.pointerBased;

import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshOptimizer;

public class PMeshBuilder implements IMeshBuilder<PVertex, PHalfEdge, PFace> {
    PMeshDataStorage meshDataStorage;
    PMesh mesh;

    public PMeshBuilder(PMeshWithDataStorage toCopy) {
        /*IMeshWithDataStorage<AVertex, AHalfEdge, AFace> cloned = toCopy.clone();
        meshDataStorage = (PMeshDataStorage) cloned.getDataStorage();
        mesh = (PMesh) cloned.getMesh();*/

        // todo hh: refactor this to a clone when making the mesh immutable
        meshDataStorage = (PMeshDataStorage) toCopy.getDataStorage();
        mesh = (PMesh) toCopy.getMesh();
    }


    @Override
    public IMeshOptimizer<PVertex, PHalfEdge, PFace> getOptimizer() {
        return new PMeshBuilderOptimizer(this);
    }

    @Override
    public IMeshWithDataStorage<PVertex, PHalfEdge, PFace> build() {
        return new PMeshWithDataStorage(mesh, meshDataStorage);
    }
}
