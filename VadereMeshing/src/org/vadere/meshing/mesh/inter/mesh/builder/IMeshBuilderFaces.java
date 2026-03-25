package org.vadere.meshing.mesh.inter.mesh.builder;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.List;

/**
 * Part of the {@link IMeshBuilder} used to create {@link IFace}
 */
public interface IMeshBuilderFaces<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    /**
     * A factory method which creates a new face which will be added to the mesh data structure.
     *
     * @return a face
     */
    F createAndInsert();

    F createAndInsertHole();

    /**
     * <p>A factory method which creates a new face from a list {@link List}
     * of vertices in the mesh which forms a simple (non-intersecting) polygon.
     * All base elements of the face and the face itself will be
     * added to the mesh data structure.</p>
     *
     * <p>Assumption:
     * - points (p1, ..., pn) is a valid simple polygon nd p1 != pn and
     * - vertices are already added to the mesh data structure!</p>
     *
     * @param points a list {@link List} of vertices representing a simple polygon (non-intersecting)
     * @return a face
     */
    F createFromVertexesInTheMesh(@NotNull final List<V> points);

    /**
     * <p>A factory method which creates a new face from a list {@link List}
     * of vertices in the mesh which forms a simple (non-intersecting) polygon.
     * All base elements of the face and the face itself will be
     * added to the mesh data structure.</p>
     *
     * <p>Assumption:
     * - points (p1, ..., pn) is a valid simple polygon nd p1 != pn and
     * - vertices are already added to the mesh data structure!</p>
     *
     * @param points an array of vertices representing a simple polygon (non-intersecting)
     * @return a face
     */
    F createFromVertexesInTheMesh(@NotNull final V... points);

    /**
     * A factory method which creates a new face from an array
     * of points which forms a simple (non-intersecting) polygon.
     * All base elements of the face and the face itself will be
     * added to the mesh data structure.
     *
     * Assumption: points (p1, ..., pn) is a valid simple polygon
     * and p1 != pn.
     *
     * @param points an array of points representing a simple polygon (non-intersecting)
     * @return a face
     */
    F createAndInsert(@NotNull final IPoint... points);

    /**
     * A factory method which creates a new face from an list  {@link List}
     * of points which forms a simple (non-intersecting) polygon.
     * All base elements of the face and the face itself will be
     * added to the mesh data structure.
     *
     * Assumption: points (p1, ..., pn) is a valid simple polygon
     * and p1 != pn.
     *
     * @param points a list {@link List} of points representing a simple polygon (non-intersecting)
     * @return a face
     */
    F createAndInsertFromList(@NotNull final List<IPoint> points);

    /**
     * Sets (uni-directional) the half-edge of the face. This is uni-directional,
     * i.e. this will not set the face of the half-edge!
     *
     * @param edge  the half-edge
     * @param face  the face
     */
    void setEdge(@NotNull final F face, @NotNull final E edge);

    /**
     * Marks the face to be a hole.
     *
     * @param face a face
     */
    void convertToHole(@NotNull final F face);

    /**
     * Marks the face to be destroyed when {@link IMeshBuilder#getOptimizer()}'s {@link IMeshOptimizer#garbageCollection()} is called.
     * @param face a face
     */
    void destroy(@NotNull final F face);
}
