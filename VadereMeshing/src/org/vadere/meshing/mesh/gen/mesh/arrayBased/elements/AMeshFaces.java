package org.vadere.meshing.mesh.gen.mesh.arrayBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshFaces;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AMeshFaces implements IMeshFaces<AVertex, AHalfEdge, AFace>, Iterable<AFace>{
    protected IMesh<AVertex, AHalfEdge, AFace>  parent;

    List<AFace> items;
    int numberOfFaces;
    int numberOfHoles;
    List<AFace> holes;
    AFace outerBorder;

    public AMeshFaces(IMesh<AVertex, AHalfEdge, AFace> parent) {
        this.parent = parent;
        clear();
    }

    /**
     * Copy constructor
     */
    public AMeshFaces(IMesh<AVertex, AHalfEdge, AFace> parent, AMeshFaces toCopy) {
        this(parent);

        this.items = toCopy.items.stream().map(AFace::new).collect(Collectors.toList());
        this.holes = toCopy.holes.stream().map(AFace::new).collect(Collectors.toList());
        this.outerBorder = new AFace(toCopy.outerBorder);
        this.numberOfFaces = toCopy.numberOfFaces;
        this.numberOfHoles = toCopy.numberOfHoles;
    }

    public void clear() {
        this.items = new ArrayList<>();
        this.holes = new ArrayList<>();
        this.outerBorder = new AFace(-1, true);
        this.numberOfFaces = 0;
        this.numberOfHoles = 0;
    }

    @Override
    public IMesh<AVertex, AHalfEdge, AFace> base() {
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
    public AFace getFirst() {
        return items.stream().filter(f -> !isDestroyed(f)).filter(f -> !isBoundary(f)).findAny().get();
    }

    @Override
    public AFace getOf(@NotNull AHalfEdge halfEdge) {
        int edgeId = halfEdge.getFaceId();
        if (edgeId == -1) {
            if (halfEdge.isDestroyed()) {
                throw new IllegalArgumentException(halfEdge + " is already destroyed.");
            }
            return outerBorder;
        } else {
            return items.get(halfEdge.getFaceId());
        }
    }

    @Override
    public boolean isBoundary(@NotNull AFace face) {
        return face.isBoundary();
    }

    @Override
    public AFace getOuterBorder() {
        return outerBorder;
    }

    @Override
    public boolean isHole(@NotNull AFace face) {
        return isBoundary(face) && face != outerBorder;
    }

    @Override
    public boolean isDestroyed(@NotNull AFace face) {
        return face.isDestroyed();
    }

    @Override
    public Stream<AFace> stream(@NotNull Predicate<AFace> predicate) {
        return items.stream().filter(f -> isAlive(f)).filter(predicate);
    }

    @Override
    public Stream<AFace> streamHoles() {
        return holes.stream().filter(f -> !isDestroyed(f));
    }

    @NotNull
    @Override
    public Iterator<AFace> iterator() {
        return getAll().iterator();
    }
}
