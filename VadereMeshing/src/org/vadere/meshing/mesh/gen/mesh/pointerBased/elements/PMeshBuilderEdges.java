package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshBuilderEdges;

public class PMeshBuilderEdges<Vertices extends PMeshVertices, Edges extends PMeshEdges, Faces extends PMeshFaces,
        Mesh extends PointerBasedMesh<Vertices,Edges,Faces>> implements IMeshBuilderEdges<PVertex, PHalfEdge, PFace> {
    private final PMeshEdges edges;

    public PMeshBuilderEdges(MeshBuilderBase<PVertex, PHalfEdge, PFace, PMeshDataStorage, Mesh> parent) {
        edges = parent.getMesh().edges();
    }

    private void addEdge(@NotNull PHalfEdge edge) {
        assert !edges.items.contains(edge);
        edges.items.add(edge);
        edges.numberOfEdges++;
    }

    @Override
    public PHalfEdge createAndInsert(@NotNull final PVertex vertex) {
        PHalfEdge edge = new PHalfEdge(vertex);
        addEdge(edge);
        return edge;
    }

    @Override
    public PHalfEdge createAndInsert(@NotNull final PVertex vertex, @NotNull final PFace face) {
        PHalfEdge edge = new PHalfEdge(vertex, face);
        addEdge(edge);
        return edge;
    }

    @Override
    public void setTwin(@NotNull final PHalfEdge halfEdge, @NotNull final PHalfEdge twin) {
        halfEdge.setTwin(twin);
        twin.setTwin(halfEdge);
    }

    @Override
    public void setNext(@NotNull final PHalfEdge halfEdge, @NotNull final PHalfEdge next) {
        halfEdge.setNext(next);
        next.setPrevious(halfEdge);
    }

    @Override
    public void setPrev(@NotNull final PHalfEdge halfEdge, @NotNull final PHalfEdge prev) {
        halfEdge.setPrevious(prev);
        prev.setNext(halfEdge);
    }

    @Override
    public void setFace(@NotNull final PHalfEdge halfEdge, @NotNull final PFace face) {
        halfEdge.setFace(face);
    }

    @Override
    public void setVertex(@NotNull final PHalfEdge halfEdge, @NotNull final PVertex vertex) {
        halfEdge.setEnd(vertex);
    }

    @Override
    public void destroy(@NotNull final PHalfEdge edge) {
        edge.destroy();
        edges.numberOfEdges--; // we destroy the edge and its twin!
    }
}
