package org.vadere.meshing.mesh.inter.mesh.builder;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

/**
 * Part of the {@link IMeshBuilder} used to create {@link org.vadere.meshing.mesh.inter.mesh.IMeshEdges}
 */
public interface IMeshBuilderEdges<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    /**
     * A factory method which creates a new half-edge with a established vertex relationship.
     * The the half-edge of the vertex will not change.
     * This edge will be added to the mesh data structure.
     *
     * @param vertex the vertex of the edge
     * @return a half-edge
     */
    E createAndInsert(@NotNull final V vertex);

    /**
     * A factory method which creates a new half-edge with a established vertex and face relationship.
     * The the half-edge of the face or the vertex will not change.
     * This edge will be added to the mesh data structure.
     *
     * @param vertex the vertex of the edge
     * @param face the face of the edge
     * @return a half-edge
     */
    E createAndInsert(@NotNull final V vertex, @NotNull final F face);

    /**
     * Sets (uni-directional) the face of a half-edge. This is uni-directional,
     * i.e. this will not set the half-edge of the face!
     *
     * @param halfEdge  the half-edge
     * @param face      the face
     */
    void setFace(@NotNull final E halfEdge, @NotNull final F face);

    /**
     * Sets the bi-directional relation twin of these two half-edges,
     * i.e. the halfedge will be the twin of the twin and vise versa.
     *
     * @param halfEdge  the half-edge halfedge (the twin of the twin)
     * @param twin      the half-edge twin (the twin of the half-edge)
     */
    void setTwin(@NotNull final E halfEdge, @NotNull final E twin);

    /**
     * Sets the bi-directional relation next-prev of these two half-edges,
     * i.e. the next will be the next (successor) of the halfedge and the halfedge will
     * be the prev (predecessor) of next.
     *
     * @param halfEdge  the half-edge halfedge (the next of the next)
     * @param next      the half-edge next (the next of the half-edge)
     */
    void setNext(@NotNull final E halfEdge, @NotNull final E next);

    /**
     * Sets the bi-directional relation next-prev of these two half-edges,
     * i.e. the prev will be the prev (predecessor) of the halfedge and the halfedge will
     * be the next (successor) of prev.
     *
     * @param halfEdge  the half-edge halfedge (the next of the next)
     * @param prev      the half-edge prev (the next of the half-edge)
     */
    void setPrev(@NotNull final E halfEdge, @NotNull final E prev);

    /**
     * Sets (uni-directional) the vertex of a half-edge. This is uni-directional,
     * i.e. this will not set the half-edge of the vetex!
     *
     * @param halfEdge    the half-edge
     * @param vertex      the vertex
     */
    void setVertex(@NotNull final E halfEdge, @NotNull final V vertex);

    /**
     * Marks the edge to be destroyed when {@link IMeshBuilder#getOptimizer()}'s {@link IMeshOptimizer#garbageCollection()} is called.
     *
     * @param edge a half-edge
     */
    void destroy(@NotNull final E edge);
}
