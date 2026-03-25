package org.vadere.meshing.mesh.inter.mesh.builder;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class MeshBuilderFacesBase<V extends IVertex, E extends IHalfEdge, F extends IFace, Mesh extends IMesh<V, E, F>>
        implements IMeshBuilderFaces<V, E, F> {
    private final IMeshBuilder<V, E, F> parent;

    public MeshBuilderFacesBase(IMeshBuilder<V, E, F> parent) {
        this.parent = parent;
    }

    @SafeVarargs
    @Override
    public final F createFromVertexesInTheMesh(@NotNull final V... points) {
        return createFromVertexesInTheMesh(Lists.newArrayList(points));
    }

    @Override
    public F createAndInsert(@NotNull final IPoint... points) {
        return createFromVertexesInTheMesh(Arrays.stream(points).map(p -> parent.vertices().createAndInsert(p)).collect(Collectors.toList()));
    }

    @Override
    public F createAndInsertFromList(@NotNull final List<IPoint> points) {
        return createFromVertexesInTheMesh(points.stream().map(p -> parent.vertices().createAndInsert(p)).collect(Collectors.toList()));
    }

    @Override
    public F createFromVertexesInTheMesh(@NotNull final List<V> points) {
        assert parent.getMesh().vertices().getAll().containsAll(points);

        F face = createAndInsert();
        F borderFace = parent.getMesh().faces().getOuterBorder();

        LinkedList<E> edges = new LinkedList<>();
        LinkedList<E> borderEdges = new LinkedList<>();
        for(V p : points) {
            E edge = parent.edges().createAndInsert(p, face);
            parent.vertices().setEdge(p, edge);
            E borderEdge = parent.edges().createAndInsert(p, borderFace);
            edges.add(edge);
            borderEdges.add(borderEdge);
        }

        E edge = null;
        for(E halfEdge : edges) {
            if(edge != null) {
                parent.edges().setNext(edge, halfEdge);
            }
            edge = halfEdge;
        }
        parent.edges().setNext(edges.peekLast(), edges.peekFirst());

        edge = null;
        for(E halfEdge : borderEdges) {
            if(edge != null) {
                parent.edges().setPrev(edge, halfEdge);
            }
            edge = halfEdge;
        }
        parent.edges().setPrev(borderEdges.peekLast(), borderEdges.peekFirst());

        for(int i = 0; i < edges.size(); i++) {
            E halfEdge = edges.get(i);
            E twin = borderEdges.get((i + edges.size() - 1) % edges.size());
            parent.edges().setTwin(halfEdge, twin);
        }

        setEdge(face, edges.peekFirst());
        setEdge(borderFace, borderEdges.peekFirst());

        return face;
    }
}
