package org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles;

import org.vadere.meshing.mesh.gen.mesh.MeshWithDataStorageBase;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PFace;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.PVertex;
import org.vadere.meshing.mesh.inter.mesh.ITriangleMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

public class PTriangleMeshWithDataStorage extends MeshWithDataStorageBase<PVertex, PHalfEdge, PFace, PTriangleMesh>
    implements ITriangleMeshWithDataStorage<PVertex, PHalfEdge, PFace>
{
    public PTriangleMeshWithDataStorage(PTriangleMesh mesh, IMeshDataStorage<PVertex, PHalfEdge, PFace> dataStorage) {
        super(mesh, dataStorage);
    }

    @Override
    public ITriangleMeshWithDataStorage<PVertex, PHalfEdge, PFace> clone() {
        PTriangleMesh clone = getMesh().copy();
        return new PTriangleMeshWithDataStorage(clone, getDataStorage().clone(clone));
    }

    @Override
    public ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> modify() {
        return new PTriangleMeshBuilder(this);
    }
}
