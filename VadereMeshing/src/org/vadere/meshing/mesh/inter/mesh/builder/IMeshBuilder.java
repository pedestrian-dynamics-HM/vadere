package org.vadere.meshing.mesh.inter.mesh.builder;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;
import org.vadere.meshing.mesh.inter.meshConnectivity.IPolyConnectivity;


/**
 * The IMeshBuilder interface defines the blueprint for creating and managing a mesh data structure.
 * A mesh is composed of vertices, half-edges, and faces, which together form a complex structure
 * that represents surfaces or shapes in space. This interface provides various methods to access
 * and modify the mesh's underlying topology and data.
 *
 * Original author: Benedikt Zoennchen
 * Refactored by: Hayato Hess
 */
public interface IMeshBuilder<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    IMesh<V, E, F> getMesh();
    IMeshDataStorage<V, E, F> getDataStorage();

    IMeshBuilderEdges<V, E, F> edges();
    IMeshBuilderVertices<V, E, F> vertices();
    IMeshBuilderFaces<V, E, F> faces();

    IMeshOptimizer<V, E, F> getOptimizer();
    IMeshWithDataStorage<V, E, F> getMeshWithDataStorage();

    IMeshBuilder<V, E, F> copy();

    IPolyConnectivity<V, E, F> changeConnectivity();

    /**
     * Clears the mesh data structure i.e. after this call the mesh and its data is empty.
     */
    void clear();

    IMeshBuilder<V, E, F> newInstance();
}
