package org.vadere.meshing.mesh.inter.mesh.builder;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.meshConnectivity.ITriConnectivity;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.List;

public interface IMeshBuilderVertices<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    /**
     * A factory method which creates a new vertex. The vertex will not be inserted into the mesh data
     * structure.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return a vertex.
     */
    V create(final double x, final double y);

    /**
     * A factory method which creates a new vertex. The vertex will not be inserted into the mesh data
     * structure.
     *
     * @param point a container supporting 2D-coordinates
     * @return a vertex.
     */
    V create(@NotNull final IPoint point);

    /**
     * Inserts the vertex into the mesh data structure.
     *
     * @param vertex the vertex
     */
    void insert(@NotNull final V vertex);


    /**
     * Inserts the point into the mesh data structure, returning its vertex
     *
     * @param point the point
     * @return the vertex of the point
     */
    default V createAndInsert(final IPoint point) {
        V vertex = create(point);
        insert(vertex);
        return vertex;
    }

    /**
     * Inserts the vertex into the mesh data structure.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return the vertex of the point defiend by (x,y)
     */
    default V createAndInsert(final double x, final double y) {
        V vertex = create(x, y);
        insert(vertex);
        return vertex;
    }

    /**
     * Sets (uni-directional) the half-edge of a vertex. This is uni-directional,
     * i.e. this will not set the vertex of the half-edge!
     *
     * @param vertex    the half-edge
     * @param edge      the face
     */
    void setEdge(@NotNull final V vertex, @NotNull final E edge);

    /**
     * Sets the point of a vertex. This should only be used with great care since
     * this will re-position the vertex and may destroy a valid connectivity! So in
     * general this can only be done if the new point is contained in the convex hull
     * of the neighbouring vertices of the vertex.
     *  @param vertex    the vertex
     * @param point     its new point
     */
    void setPoint(@NotNull final V vertex, @NotNull final IPoint point);

    void setCoords(@NotNull V vertex, double x, double y);

    /**
     * Marks the vertex to be destroyed when {@link IMeshBuilder#getOptimizer()}'s {@link IMeshOptimizer#garbageCollection()} is called.
     *
     * @param vertex a vertex
     */
    void destroy(@NotNull final V vertex);

    /**
     * Sets the positions of all vertices (ignoring destroyed ones)
     *
     * @param positions positions in the same order as the vertices
     */
    void setAllVertexPositions(final List<IPoint> positions);

    /**
     * This method is for synchronizing resources if multiple threads are used.
     * It tries to lock the vertex which might be uses to modify the mesh data structure
     * by multiple threads e.g. one can flip an edge see {@link ITriConnectivity#flipSync(IHalfEdge)}
     * in parallel by locking all 4 involved vertices beforehand.
     *
     * @param vertex the vertex for which the lock is acquired
     * @return true if the lock was successfully acquired, false otherwise
     */
    boolean tryLock(@NotNull final V vertex);

    /**
     * This method is for synchronizing resources if multiple threads are used.
     * It releases the lock if it was acquired otherwise this method has no effect.
     *
     * @param vertex the vertex for which the lock is released
     */
    void unlock(@NotNull final V vertex);
}
