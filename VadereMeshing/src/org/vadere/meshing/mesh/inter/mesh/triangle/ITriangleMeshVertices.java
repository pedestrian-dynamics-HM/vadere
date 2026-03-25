package org.vadere.meshing.mesh.inter.mesh.triangle;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IMeshVertices;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

public interface ITriangleMeshVertices<V extends IVertex, E extends IHalfEdge, F extends IFace> extends IMeshVertices<V, E, F> {

    /**
     * Retrieves the vertex in a triangle that is not part of the edge
     */
    default V getOpposite(@NotNull E edge) {
        assert parent().edges().getAllOf(parent().faces().getOf(edge)).size() == 3;
        return getEndOf(parent().edges().getNext(edge));
    }

}
