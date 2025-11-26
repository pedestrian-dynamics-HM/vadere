package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshFaces;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PMeshFaces implements IMeshFaces<PVertex, PHalfEdge, PFace> {
    IMesh<PVertex, PHalfEdge, PFace> parent;
    int numberOfFaces;
    int numberOfHoles;
    List<PFace> items;
    List<PFace> holes;
    PFace outerBorder;

    public PMeshFaces(IMesh<PVertex, PHalfEdge, PFace> parent) {
        this.parent = parent;
    }

    public void clear() {
        outerBorder = new PFace(true);
        numberOfFaces = 0;
        numberOfHoles = 0;
        items = new ArrayList<>();
        holes = new ArrayList<>();
    }

    @Override
    public IMesh<PVertex, PHalfEdge, PFace> base() {
        return parent;
    }

    @Override
    public int count() {
        return numberOfFaces;
    }

    @Override
    public int getNumberOfHoles() {
        return numberOfHoles;
    }

    @Override
    public PFace getFirst() {
        return items.stream().filter(face -> !isDestroyed(face)).filter(f -> !isBoundary(f)).findAny().get();
    }

    @Override
    public PFace getOf(@NotNull PHalfEdge halfEdge) {
        return halfEdge.getFace();
    }

    @Override
    public boolean isBoundary(@NotNull PFace face) {
        return face.isBoundary();
    }

    @Override
    public PFace getOuterBorder() {
        return outerBorder;
    }

    @Override
    public boolean isHole(@NotNull PFace face) {
        return isBoundary(face) && face != outerBorder;
    }

    @Override
    public boolean isDestroyed(@NotNull PFace face) {
        return face.isDestroyed();
    }

    @Override
    public Stream<PFace> stream(@NotNull Predicate<PFace> predicate) {
        return items.stream().filter(f -> !isDestroyed(f)).filter(predicate);
    }

    @Override
    public Stream<PFace> streamHoles() {
        return holes.stream().filter(h -> !isDestroyed(h));
    }

    public <Vertices extends PMeshVertices, Edges extends PMeshEdges, Faces extends PMeshFaces> void copyFrom(PointerBasedMesh<Vertices, Edges, Faces> dataToCopy, Map<PFace, PFace> faceMap, Map<PHalfEdge, PHalfEdge> edgeMap) {
        numberOfHoles = dataToCopy.faces().numberOfHoles;
        numberOfFaces = dataToCopy.faces().numberOfFaces;

        outerBorder = new PFace(dataToCopy.faces().outerBorder);
        faceMap.put(dataToCopy.faces().outerBorder, outerBorder);
        outerBorder.setEdge(edgeMap.get(outerBorder.getEdge()));

        for(PFace f : dataToCopy.faces().items) {
            PFace copiedFace = new PFace(f);
            faceMap.put(f, copiedFace);
            copiedFace.setEdge(edgeMap.get(f.getEdge()));
            items.add(copiedFace);
            if(isHole(f)) {
                holes.add(copiedFace);
            }
        }
    }

    @NotNull
    @Override
    public Iterator<PFace> iterator() {
        return getAll().iterator();
    }
}
