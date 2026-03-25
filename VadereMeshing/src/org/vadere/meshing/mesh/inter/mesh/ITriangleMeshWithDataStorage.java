package org.vadere.meshing.mesh.inter.mesh;
import org.vadere.meshing.mesh.inter.mesh.builder.ITriangleMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;

/**
 * Container holding a Triangle mesh with it's DataStorage
 */
public interface ITriangleMeshWithDataStorage<V extends IVertex, E extends IHalfEdge, F extends IFace>
        extends IMeshWithDataStorage<V, E, F>{
    ITriangleMesh<V, E, F> getMesh();
    ITriangleMeshWithDataStorage<V, E, F> clone();
    ITriangleMeshBuilder<V, E, F> modify();
}
