package org.vadere.meshing.mesh.gen.mesh.arrayBased;

import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshOptimizer;

public class AMeshBuilder implements IMeshBuilder<AVertex, AHalfEdge, AFace> {
    AMeshDataStorage meshDataStorage;
    AMesh mesh;

    public AMeshBuilder(AMeshWithDataStorage toCopy) {
        /*IMeshWithDataStorage<AVertex, AHalfEdge, AFace> cloned = toCopy.clone();
        meshDataStorage = (AMeshDataStorage) cloned.getDataStorage();
        mesh = (AMesh) cloned.getMesh();*/

        // todo hh: refactor this to a clone when making the mesh immutable
        meshDataStorage = (AMeshDataStorage) toCopy.getDataStorage();
        mesh = (AMesh) toCopy.getMesh();
    }

    public IMeshOptimizer<AVertex, AHalfEdge, AFace> getOptimizer() {
        return new AMeshBuilderOptimizer(this);
    }

    public AMeshWithDataStorage build(){
        return new AMeshWithDataStorage(mesh, meshDataStorage);
    }
}
