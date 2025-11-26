package org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles;

import org.vadere.meshing.mesh.gen.mesh.MeshWithDataStorageBase;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AFace;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AVertex;
import org.vadere.meshing.mesh.inter.mesh.ITriangleMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.MeshUtils;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

public class ATriangleMeshWithDataStorage extends MeshWithDataStorageBase<AVertex, AHalfEdge, AFace, ATriangleMesh>
        implements ITriangleMeshWithDataStorage<AVertex, AHalfEdge, AFace> {

    public ATriangleMeshWithDataStorage(ATriangleMesh mesh, IMeshDataStorage<AVertex, AHalfEdge, AFace> dataStorage) {
        super(mesh, dataStorage);
    }

    @Override
    public ITriangleMeshWithDataStorage<AVertex, AHalfEdge, AFace> clone() {
        ATriangleMesh clone = getMesh().copy();
        return new ATriangleMeshWithDataStorage(clone, getDataStorage().clone(clone));
    }

    @Override
    public ITriangleMeshBuilder<AVertex, AHalfEdge, AFace> modify() {
        return new ATriangleMeshBuilder(this);
    }

    /**
     * <p>Creates a very simple mesh consisting of two triangles ((-100, 0), (100, 0), (0, 1)) and ((0, -1), (-100, 0), (100, 0)).</p>
     *
     * @return the created mesh
     */
    public static ATriangleMeshWithDataStorage createSimpleTriMesh() {
        ATriangleMeshBuilder meshBuilder = new ATriangleMeshBuilder();
        MeshUtils.createSimpleTriMesh(meshBuilder);
        return (ATriangleMeshWithDataStorage) meshBuilder.getMeshWithDataStorage();
    }
}
