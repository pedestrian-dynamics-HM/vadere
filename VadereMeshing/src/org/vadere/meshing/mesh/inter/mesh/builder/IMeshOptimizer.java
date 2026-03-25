package org.vadere.meshing.mesh.inter.mesh.builder;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

public interface IMeshOptimizer<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    /**
     * Rearranges the memory location of faces, vertices and halfEdges of the mesh according to
     * the {@link Iterable} faceOrder. I.e. edges, vertices and faces which are close the faceOrder
     * will be close in the memory!
     *
     * Assumption: faceOrder contains all faces of this mesh.
     * Invariant: the geometry i.e. the connectivity and the vertex positions will not change.
     *
     * @param faceOrder the new order
     */
    void arrangeMemory(@NotNull final Iterable<F> faceOrder);

    /**
     * Removes deleted base elements from this data structure.
     * This might be used if removing an element from the mesh does not removes
     * this element from the data structure i.e. the mesh representing the geometry.
     * This operation might be expensive O(n) for n points.
     */
    void garbageCollection();
}
