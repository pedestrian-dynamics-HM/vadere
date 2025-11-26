package org.vadere.meshing.mesh.inter.mesh.triangle;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.ITriEventListener;
import org.vadere.meshing.mesh.inter.mesh.*;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyTriConnectivity;

public interface ITriangleMesh<V extends IVertex, E extends IHalfEdge, F extends IFace> extends IMesh<V, E, F> {
    ITriangleMeshEdges<V, E, F> edges();
    ITriangleMeshFaces<V, E, F> faces();
    ITriangleMeshVertices<V, E, F> vertices();

    IReadOnlyTriConnectivity<V, E, F> readConnectivity();

    void addTriEventListener(@NotNull ITriEventListener<V, E, F> triEventListener);
    void removeTriEventListener(@NotNull ITriEventListener<V, E, F> triEventListener);


    /**
     * <p>Informs the mesh when one of its triangle / face is split into three faces
     * and inform all listeners about that event.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param original  the original face
     * @param f1        one of the split results
     * @param f2        one of the split results
     * @param f3        one of the split results
     * @param v         the vertex inserted
     */
    void splitTriangleEvent(@NotNull final F original, @NotNull final F f1, @NotNull final F f2, @NotNull final F f3, @NotNull final V v);

    /**
     * <p>Informs the mesh when one of its triangle / face is split at a specific edge which
     * will split it into tow faces. The method informs all listeners about that event.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param originalEdge  the original edge which is split
     * @param original      the original face
     * @param f1            one of the split results
     * @param f2            one of the split results
     * @param v             the vertex inserted
     */
    void splitEdgeEvent(@NotNull E originalEdge, @NotNull final F original, @NotNull final F f1, @NotNull final F f2, @NotNull final V v);

    /**
     * <p>Informs the mesh when one of its triangle / face edge is flipped
     * and inform all listeners about that event. For each flip two
     * triangles are taking part.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param f1 the first triangle / face of the flip operation
     * @param f2 the second triangle / face of the flip operation
     */
    void flipEdgeEvent(@NotNull final F f1, @NotNull final F f2);

    /**
     * <p>Informs the mesh when a new point is inserted into the mesh.</p>
     *
     * <p>Does not change the connectivity.</p>
     *
     * @param vertex the vertex which was inserted
     */
    void insertEvent(@NotNull final E vertex);
}
