package org.vadere.meshing.mesh.gen.mesh.arrayBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.AMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.MeshBuilderVerticesBase;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;


public class AMeshBuilderVertices<Vertices extends AMeshVertices, Edges extends AMeshEdges, Faces extends AMeshFaces,
        Mesh extends ArrayBasedMesh<Vertices,Edges,Faces>> extends MeshBuilderVerticesBase<AVertex, AHalfEdge, AFace, Mesh> {
    private final MeshBuilderBase<AVertex, AHalfEdge, AFace, AMeshDataStorage, Mesh> parent;
    private final AMeshVertices vertices;

    public AMeshBuilderVertices(MeshBuilderBase<AVertex, AHalfEdge, AFace, AMeshDataStorage, Mesh> parent) {
        super(parent);
        this.parent = parent;
        vertices = parent.getMesh().vertices();
    }

    @Override
    public AVertex create(final double x, final double y) {
        return create(parent.getMesh().createPoint(x, y));
    }

    @Override
    public AVertex create(@NotNull final IPoint point) {
        int id = vertices.items.size();
        parent.getDataStorage().onVertexCreated();
        return new AVertex(id, point);
    }

    public void insert(@NotNull final AVertex vertex) {
        if (vertex.getId() != vertices.items.size()) {
            throw new IllegalArgumentException(vertex.getId() + " != " + vertices.items.size());
        } else {
            vertices.numberOfVertices++;
            vertices.items.add(vertex);
        }
    }

    @Override
    public void setEdge(@NotNull AVertex vertex, @NotNull AHalfEdge edge) {
        assert edge.getEnd() == vertex.getId();
        if(edge.getEnd() != vertex.getId()) {
            throw new IllegalArgumentException("end of the edge is not equals to the vertex:" + vertex.getId() + " != " + edge.getEnd());
        }
        vertex.setEdge(edge.getId());
    }

    @Override
    public void setPoint(@NotNull final AVertex vertex, @NotNull final IPoint point) {
        vertex.setPoint(point);
    }

    @Override
    public void setCoords(@NotNull AVertex vertex, double x, double y) {
        vertex.setPoint(new VPoint(x, y));
    }

    @Override
    public void destroy(@NotNull final AVertex vertex) {
        if (!vertices.isDestroyed(vertex)) {
            parent.getMesh().setElementRemoved(true);
            vertices.numberOfVertices--;
            vertex.destroy();
        }
    }

    @Override
    public boolean tryLock(@NotNull AVertex vertex) {
        return vertex.getLock().tryLock();
    }

    @Override
    public void unlock(@NotNull AVertex vertex) {
        vertex.getLock().unlock();
    }
}
