package org.vadere.meshing.mesh.gen.mesh.arrayBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshEdges;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AMeshEdges implements IMeshEdges<AVertex, AHalfEdge, AFace> {
    protected IMesh<AVertex, AHalfEdge, AFace>  parent;

    List<AHalfEdge> items;
    int numberOfEdges;

    public AMeshEdges(IMesh<AVertex, AHalfEdge, AFace> parent) {
        this.parent = parent;
        clear();
    }

    /**
     * Copy constructor
     */
    public AMeshEdges(IMesh<AVertex, AHalfEdge, AFace> parent, AMeshEdges toCopy) {
        this(parent);

        this.items = toCopy.items.stream().map(AHalfEdge::new).collect(Collectors.toList());
        this.numberOfEdges = toCopy.numberOfEdges;
    }

    public void clear() {
        this.items = new ArrayList<>();
        this.numberOfEdges = 0;
    }

    @Override
    public IMesh<AVertex, AHalfEdge, AFace> base() {
        return this.parent;
    }

    @Override
    public int count() {
        return numberOfEdges;
    }

    @Override
    public AHalfEdge getOf(@NotNull AVertex vertex) {
        if(vertex.getEdge() == -1) {
            return null;
        }
        return items.get(vertex.getEdge());
    }

    @Override
    public AHalfEdge getNext(@NotNull AHalfEdge halfEdge) {
        if(halfEdge.getNext() == -1) {
            return null;
        }
        return items.get(halfEdge.getNext());
    }

    @Override
    public AHalfEdge getPrev(@NotNull AHalfEdge halfEdge) {
        if(halfEdge.getPrevious() == -1) {
            return null;
        }
        return items.get(halfEdge.getPrevious());
    }

    @Override
    public AHalfEdge getTwin(@NotNull AHalfEdge halfEdge) {
        if(halfEdge.getTwin() == -1) {
            return null;
        }
        return items.get(halfEdge.getTwin());
    }

    @Override
    public boolean isBoundary(@NotNull AHalfEdge halfEdge) {
        boolean isBorder = halfEdge.getFaceId() == base().faces().getOuterBorder().getId();
        boolean isBoundary = base().faces().isBoundary(base().faces().getOf(halfEdge));
        return isBorder || isBoundary;
    }

    @Override
    public AHalfEdge getAnyOf(@NotNull AFace face) {
        return items.get(face.getEdge());
    }

    @Override
    public IPoint getMutableEndPoint(@NotNull AHalfEdge halfEdge) {
        return base().vertices().getEndOf(halfEdge).getPoint();
    }

    @Override
    public boolean isDestroyed(@NotNull AHalfEdge edge) {
        return edge.isDestroyed();
    }

    @Override
    public Stream<AHalfEdge> stream() {
        return items.stream().filter(e -> !isDestroyed(e));
    }

    @Override
    public Stream<AHalfEdge> streamParallel() {
        return items.parallelStream().filter(e -> !e.isDestroyed());
    }

    @NotNull
    @Override
    public Iterator<AHalfEdge> iterator() {
        return stream().iterator();
    }
}
