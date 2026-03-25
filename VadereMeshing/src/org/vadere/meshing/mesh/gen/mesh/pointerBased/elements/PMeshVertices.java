package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshVertices;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.*;
import java.util.stream.Stream;

public class PMeshVertices implements IMeshVertices<PVertex, PHalfEdge, PFace> {
    IMesh<PVertex, PHalfEdge, PFace> parent;
    List<PVertex> items;
    int numberOfVertices;

    public PMeshVertices(IMesh<PVertex, PHalfEdge, PFace> parent) {
        this.parent = parent;
    }

    public void clear() {
        this.items = new ArrayList<>();
        this.numberOfVertices = 0;
    }

    @Override
    public IMesh<PVertex, PHalfEdge, PFace> parent() {
        return parent;
    }

    @Override
    public PVertex getEndOf(@NotNull PHalfEdge halfEdge) {
        return halfEdge.getEnd();
    }

    @Override
    public double getX(@NotNull final PVertex vertex) {
        return vertex.getX();
    }

    @Override
    public double getY(@NotNull final PVertex vertex) {
        return vertex.getY();
    }

    @Override
    public IPoint toMutablePoint(@NotNull PVertex vertex) {
        return vertex.getPoint();
    }

    @Override
    public boolean isDestroyed(@NotNull PVertex vertex) {
        return vertex.isDestroyed();
    }

    @Override
    public Stream<PVertex> stream() {
        return items.stream().filter(v -> !isDestroyed(v));
    }

    @Override
    public Stream<PVertex> streamParallel() {
        return items.parallelStream().filter(v -> !isDestroyed(v));
    }

    @Override
    public PVertex getRandom(@NotNull Random random) {
        int startIndex = random.nextInt(items.size());
        int index = startIndex;

        // look above
        while (index < items.size() && isDestroyed(items.get(index))) {
            index++;
        }

        // look below
        if(isDestroyed(items.get(index))) {
            index = startIndex - 1;

            while (index >= 0 && isDestroyed(items.get(index))) {
                index--;
            }
        }

        return items.get(index);
    }

    @Override
    public int count() {
        return numberOfVertices;
    }

    // vertices are not complete after this method: missing edge
    public <Vertices extends PMeshVertices, Edges extends PMeshEdges, Faces extends PMeshFaces> void beginCopyFrom(PointerBasedMesh<Vertices, Edges, Faces> dataToCopy, Map<PVertex, PVertex> vertexMap){
        numberOfVertices = dataToCopy.vertices().numberOfVertices;

        for(PVertex v : dataToCopy.vertices().items) {
            PVertex cV = v.clone();
            vertexMap.put(v, cV);

            items.add(cV);
        }
    }

    public void finalizeCopy(Map<PHalfEdge, PHalfEdge> edgeMap){
        for(PVertex clonedVertex : items) {
            clonedVertex.setEdge(edgeMap.get(clonedVertex.getEdge()));
            clonedVertex.setDown(null);
        }
    }

    @NotNull
    @Override
    public Iterator<PVertex> iterator() {
        return stream().iterator();
    }
}
