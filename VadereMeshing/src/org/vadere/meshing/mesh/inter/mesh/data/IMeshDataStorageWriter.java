package org.vadere.meshing.mesh.inter.mesh.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

public interface IMeshDataStorageWriter<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    <CV> void setData(@NotNull final V vertex, @NotNull final String name, CV data);

    default void setBooleanData(@NotNull final V vertex, @NotNull final String name, boolean data) {
        setData(vertex, name, data);
    }

    default void setDoubleData(@NotNull final V vertex, @NotNull final String name, double data) {
        setData(vertex, name, data);
    }

    default void setDoubleData(@NotNull final V vertex, @NotNull final int index, final double data) {
        setData(vertex, index+"", data);
    }

    default void setIntegerData(@NotNull final V vertex, @NotNull final String name, int data) {
        setData(vertex, name, data);
    }

    /**
     * Sets the data for a specific half-edge in O(1).
     *
     * @param edge the half-edge
     * @param name of the property
     * @param data the data
     */
    <CE> void setData(@NotNull E edge, @NotNull final String name, @Nullable CE data);

    default void setBooleanData(@NotNull E edge, @NotNull final String name, boolean data) {
        setData(edge, name, data);
    }

    default void setDoubleData(@NotNull E edge, @NotNull final String name, double data) {
        setData(edge, name, data);
    }

    default void setIntegerData(@NotNull E edge, @NotNull final String name, int data) {
        setData(edge, name, data);
    }

    /**
     * Sets the data for a specific face in O(1).
     *
     * @param face the face
     * @param data the data
     */
    <CF> void setData(@NotNull F face, @NotNull final String name, @Nullable final CF data);

    default void setBooleanData(@NotNull F face, @NotNull final String name, final boolean data) {
        setData(face, name, data);
    }

    default void setDoubleData(@NotNull F face, @NotNull final String name, final double data) {
        setData(face, name, data);
    }

    default void setIntegerData(@NotNull F face, @NotNull final String name, final int data) {
        setData(face, name, data);
    }

    void clear();
}
