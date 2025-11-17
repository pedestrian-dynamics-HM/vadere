package org.vadere.meshing.mesh.inter.mesh;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IPointLocator;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilder;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

public interface IMeshWithDataStorage<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    IMesh<V, E, F> getMesh();
    IMeshDataStorage<V, E, F> getDataStorage();

    IMeshWithDataStorage<V, E, F> clone();
    IMeshBuilder<V, E, F> toMutableMesh();
    IMeshWithDataStorage<V, E, F> toNewEmptyMeshWithDataStorage();

    /**
     * Transforms the mesh into a rich triangulation {@link IIncrementalTriangulation}.
     * There will be no connectivity changes performed!
     *
     * Assumption: The mesh is a valid triangulation.
     *
     * @return a triangulation {@link IIncrementalTriangulation} of this mesh
     */
    IIncrementalTriangulation<V, E, F> toTriangulation();

    /**
     * Transforms the mesh into a rich triangulation {@link IIncrementalTriangulation}.
     * There will be no connectivity changes performed!
     *
     * Assumption: The mesh is a valid triangulation.
     *
     * @param type  specifies the used {@link IPointLocator}
     * @return a triangulation {@link IIncrementalTriangulation} of this mesh
     */
    IIncrementalTriangulation<V, E, F> toTriangulation(@NotNull final IPointLocator.Type type);

    void clear();
}
