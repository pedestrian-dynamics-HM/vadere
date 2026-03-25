package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.inter.mesh.builder.MeshBuilderVerticesBase;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;

public class PMeshBuilderVertices<Vertices extends PMeshVertices, Edges extends PMeshEdges, Faces extends PMeshFaces,
        Mesh extends PointerBasedMesh<Vertices,Edges,Faces>> extends MeshBuilderVerticesBase<PVertex, PHalfEdge, PFace, Mesh> {
    private final PMeshVertices vertices;

    public PMeshBuilderVertices(MeshBuilderBase<PVertex, PHalfEdge, PFace, PMeshDataStorage, Mesh> parent) {
        super(parent);
        vertices = parent.getMesh().vertices();
    }

    @Override
    public PVertex create(double x, double y) {
        return create(parent.getMesh().createPoint(x, y));
    }

    @Override
    public PVertex create(@NotNull final IPoint point) {
        return new PVertex(point);
    }

    @Override
    public void insert(@NotNull final PVertex vertex) {
        vertices.numberOfVertices++;
        vertices.items.add(vertex);
    }

    @Override
    public void setEdge(@NotNull final PVertex vertex, @NotNull final PHalfEdge edge) {
        assert edge.getEnd().equals(vertex);
        if(!edge.getEnd().equals(vertex)) {
            throw new IllegalArgumentException("end of the edge is not equals to the vertex:" + vertex + " != " + edge.getEnd());
        }
        vertex.setEdge(edge);
    }

    @Override
    public void setPoint(@NotNull final PVertex vertex, @NotNull final IPoint point) {
        vertex.setPoint(point);
    }

    @Override
    public void setCoords(@NotNull PVertex vertex, double x, double y) {
        vertex.setPoint(new VPoint(x, y));
    }

    @Override
    public void destroy(@NotNull final PVertex vertex) {
        if(!vertices.isDestroyed(vertex)) {
            vertex.destroy();
            vertices.numberOfVertices--;
        }
    }

    @Override
    public boolean tryLock(@NotNull final PVertex vertex) {
        return vertex.getLock().tryLock();
    }

    @Override
    public void unlock(@NotNull final PVertex vertex) {
        vertex.getLock().unlock();
    }

}
