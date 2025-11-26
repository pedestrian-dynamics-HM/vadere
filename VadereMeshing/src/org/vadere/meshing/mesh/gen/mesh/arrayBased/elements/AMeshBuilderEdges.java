package org.vadere.meshing.mesh.gen.mesh.arrayBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.AMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilderEdges;

public class AMeshBuilderEdges<Vertices extends AMeshVertices, Edges extends AMeshEdges, Faces extends AMeshFaces,
        Mesh extends ArrayBasedMesh<Vertices,Edges,Faces>> implements IMeshBuilderEdges<AVertex, AHalfEdge, AFace> {
    private final MeshBuilderBase<AVertex, AHalfEdge, AFace, AMeshDataStorage, Mesh> parent;

    public AMeshBuilderEdges(MeshBuilderBase<AVertex, AHalfEdge, AFace, AMeshDataStorage, Mesh> parent) {
        this.parent = parent;
    }

    @Override
    public AHalfEdge createAndInsert(@NotNull AVertex vertex) {
        int id = parent.getMesh().edges().items.size();
        AHalfEdge edge = new AHalfEdge(id, vertex.getId());
        parent.getMesh().edges().items.add(edge);
        parent.getDataStorage().onEdgeCreated();
        parent.getMesh().edges().numberOfEdges++;
        return edge;
    }

    @Override
    public AHalfEdge createAndInsert(@NotNull final AVertex vertex, @NotNull final AFace face) {
        int id = parent.getMesh().edges().items.size();
        AHalfEdge edge = new AHalfEdge(id, vertex.getId(), face.getId());
        parent.getMesh().edges().items.add(edge);
        parent.getDataStorage().onEdgeCreated();
        parent.getMesh().edges().numberOfEdges++;
        return edge;
    }

    @Override
    public void setFace(@NotNull AHalfEdge halfEdge, @NotNull AFace face) {
        halfEdge.setFace(face.getId());
    }

    @Override
    public void setTwin(@NotNull AHalfEdge halfEdge, @NotNull AHalfEdge twin) {
        halfEdge.setTwin(twin.getId());
        twin.setTwin(halfEdge.getId());
    }

    @Override
    public void setNext(@NotNull AHalfEdge halfEdge, @NotNull AHalfEdge next) {
        halfEdge.setNext(next.getId());
        next.setPrevious(halfEdge.getId());
    }

    @Override
    public void setPrev(@NotNull AHalfEdge halfEdge, @NotNull AHalfEdge prev) {
        halfEdge.setPrevious(prev.getId());
        prev.setNext(halfEdge.getId());
    }

    @Override
    public void setVertex(@NotNull AHalfEdge halfEdge, @NotNull AVertex vertex) {
        halfEdge.setEnd(vertex.getId());
    }

    @Override
    public void destroy(@NotNull final AHalfEdge edge) {
        Mesh mesh = parent.getMesh();
        if (!mesh.edges().isDestroyed(edge)) {
            mesh.setElementRemoved(true);
            mesh.edges().numberOfEdges--;
            edge.destroy();
        }
    }
}
