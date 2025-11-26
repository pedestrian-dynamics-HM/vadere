package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshEdges;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class PMeshEdges implements IMeshEdges<PVertex, PHalfEdge, PFace> {
    IMesh<PVertex, PHalfEdge, PFace> parent;
    List<PHalfEdge> items;
    int numberOfEdges;

    public PMeshEdges(IMesh<PVertex, PHalfEdge, PFace> parent) {
        this.parent = parent;
    }

    public void clear() {
        this.items = new ArrayList<>();
        this.numberOfEdges = 0;
    }

    @Override
    public IMesh<PVertex, PHalfEdge, PFace> base() {
        return parent;
    }

    @Override
    public int count() {
        return numberOfEdges;
    }

    @Override
    public PHalfEdge getOf(@NotNull PVertex vertex) {
        return vertex.getEdge();
    }

    @Override
    public PHalfEdge getNext(@NotNull PHalfEdge halfEdge) {
        return halfEdge.getNext();
    }

    @Override
    public PHalfEdge getPrev(@NotNull PHalfEdge halfEdge) {
        return halfEdge.getPrevious();
    }

    @Override
    public PHalfEdge getTwin(@NotNull PHalfEdge halfEdge) {
        return halfEdge.getTwin();
    }

    @Override
    public boolean isBoundary(@NotNull PHalfEdge halfEdge) {
        return halfEdge.isBoundary();
    }

    @Override
    public PHalfEdge getAnyOf(@NotNull PFace face) {
        return face.getEdge();
    }

    @Override
    public IPoint getMutableEndPoint(@NotNull PHalfEdge halfEdge) {
        return base().vertices().getEndOf(halfEdge).getPoint();
    }

    @Override
    public boolean isDestroyed(@NotNull PHalfEdge edge) {
        return !edge.isValid();
    }

    @Override
    public Stream<PHalfEdge> stream() {
        return items.stream().filter(e -> !isDestroyed(e));
    }

    @Override
    public Stream<PHalfEdge> streamParallel() {
        return items.parallelStream().filter(e -> !isDestroyed(e));
    }

    // edges are not complete after this method: missing next, prev, twin, face
    public <Vertices extends PMeshVertices, Edges extends PMeshEdges, Faces extends PMeshFaces> void beginCopyFrom(PointerBasedMesh<Vertices, Edges, Faces> dataToCopy, Map<PVertex, PVertex> vertexMap, Map<PHalfEdge, PHalfEdge> edgeMap){
        numberOfEdges = dataToCopy.edges().numberOfEdges;

        for(PHalfEdge e : dataToCopy.edges().items) {
            PHalfEdge cE = new PHalfEdge(e);
            edgeMap.put(e, cE);
            cE.setEnd(vertexMap.get(e.getEnd()));

            items.add(cE);
        }
    }

    public void finalizeCopy(Map<PFace, PFace> faceMap, Map<PHalfEdge, PHalfEdge> edgeMap){
        for(PHalfEdge clonedEdge : items) {
            clonedEdge.setFace(faceMap.get(clonedEdge.getFace()));
            clonedEdge.setNext(edgeMap.get(clonedEdge.getNext()));
            clonedEdge.setPrevious(edgeMap.get(clonedEdge.getPrevious()));
            clonedEdge.setTwin(edgeMap.get(clonedEdge.getTwin()));
        }
    }

    @NotNull
    @Override
    public Iterator<PHalfEdge> iterator() {
        return stream().iterator();
    }
}
