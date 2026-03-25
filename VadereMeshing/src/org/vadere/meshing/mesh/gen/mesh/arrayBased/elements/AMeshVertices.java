package org.vadere.meshing.mesh.gen.mesh.arrayBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IMeshVertices;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Vertices of an array based {@link AMesh}
 *
 * Original author: Benedikt Zoennchen
 * Refactored by: Hayato Hess
 */
public class AMeshVertices implements IMeshVertices<AVertex, AHalfEdge, AFace> {
    protected IMesh<AVertex, AHalfEdge, AFace> parent;

    int numberOfVertices;
    protected List<AVertex> items;

    public AMeshVertices(IMesh<AVertex, AHalfEdge, AFace> parent) {
        this.parent = parent;
        clear();
    }

    /**
     * Copy constructor
     */
    public AMeshVertices(IMesh<AVertex, AHalfEdge, AFace> parent, AMeshVertices toCopy) {
        this(parent);
        this.items = toCopy.items.stream().map(AVertex::new).collect(Collectors.toList());
        this.numberOfVertices = toCopy.numberOfVertices;
    }

    @Override
    public IMesh<AVertex, AHalfEdge, AFace> parent() {
        return parent;
    }

    public void clear() {
        this.items = new ArrayList<>();
        this.numberOfVertices = 0;
    }

    @Override
    public AVertex getEndOf(@NotNull AHalfEdge halfEdge) {
        if(halfEdge.getEnd() == -1) {
            return null;
        }
        return items.get(halfEdge.getEnd());
    }

    @Override
    public double getX(@NotNull AVertex vertex) {
        return vertex.getX();
    }

    @Override
    public double getY(@NotNull AVertex vertex) {
        return vertex.getY();
    }

    @Override
    public IPoint toMutablePoint(@NotNull AVertex vertex) {
        return vertex.getPoint();
    }

    @Override
    public boolean isDestroyed(@NotNull AVertex vertex) {
        return vertex.isDestroyed();
    }

    @Override
    public Stream<AVertex> stream() {
        return items.stream().filter(v -> !v.isDestroyed());
    }

    @Override
    public Stream<AVertex> streamParallel() {
        return items.parallelStream().filter(v -> !v.isDestroyed());
    }

    @Override
    public AVertex getRandom(@NotNull Random random) {
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

    List<AVertex> getAllInternal(){
        return items;
    }

    @NotNull
    @Override
    public Iterator<AVertex> iterator() {
        return stream().iterator();
    }
}
