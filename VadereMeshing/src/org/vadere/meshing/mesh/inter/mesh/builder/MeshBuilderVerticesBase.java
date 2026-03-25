package org.vadere.meshing.mesh.inter.mesh.builder;

import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.List;

public abstract class MeshBuilderVerticesBase<V extends IVertex, E extends IHalfEdge, F extends IFace, Mesh extends IMesh<V, E, F>> implements IMeshBuilderVertices<V, E, F> {
    protected final IMeshBuilder<V, E, F> parent;

    public MeshBuilderVerticesBase(IMeshBuilder<V, E, F> parent) {
        this.parent = parent;
    }

    @Override
    public void setAllVertexPositions(final List<IPoint> positions) {
        if (positions.size() != parent.getMesh().vertices().count()) {
            throw new IllegalArgumentException("not equally many positions than vertices: " + positions.size() + " != " + parent.getMesh().vertices().count());
        }

        int j = 0;
        for (V vertex : parent.getMesh().vertices().getAll()) {
            if (!parent.getMesh().vertices().isDestroyed(vertex)) {
                parent.vertices().setPoint(vertex, positions.get(j));
                j++;
            }
        }
    }
}
