package org.vadere.meshing.mesh.inter.mesh.data;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

import java.util.Optional;

public interface IMeshDataStorageReader<V extends IVertex, E extends IHalfEdge, F extends IFace> {
    <CV> Optional<CV> getData(@NotNull final V vertex, @NotNull final String name, @NotNull final Class<CV> clazz);

    default boolean getBooleanData(@NotNull final V vertex, @NotNull final String name) {
        return getData(vertex, name, Boolean.class).orElse(false);
    }

    default double getDoubleData(@NotNull final V vertex, @NotNull final String name) {
        return getData(vertex, name, Double.class).orElse(0.0);
    }

    default double getDoubleData(@NotNull final V vertex, @NotNull final int index) {
        return getData(vertex, index+"", Double.class).orElse(0.0);
    }

    /**
     * Returns the data saved on the half-edge in O(1) if there is any and otherwise <tt>Optional.empty()</tt>.
     *
     * @param edge  the half-edge
     * @param name  name of the property
     * @param clazz type of the property
     * @return the data saved on the half-edge or <tt>Optional.empty()</tt> if there is no data saved
     */
    <CE> Optional<CE> getData(@NotNull E edge, @NotNull final String name, @NotNull final Class<CE> clazz);

    default boolean getBooleanData(@NotNull E edge, @NotNull final String name) {
        return getData(edge, name, Boolean.class).orElse(false);
    }

    default double getDoubleData(@NotNull E edge, @NotNull final String name) {
        return getData(edge, name, Double.class).orElse(0.0);
    }

    default int getIntegerData(@NotNull E edge, @NotNull final String name) {
        return getData(edge, name, Integer.class).orElse(0);
    }

    default int getIntegerData(@NotNull V vertex, @NotNull final String name) {
        return getData(vertex, name, Integer.class).orElse(0);
    }

    /**
     * Returns the data saved on the face in O(1) if there is any and otherwise <tt>Optional.empty()</tt>
     *
     * @param face the face
     * @param clazz
     * @return the data saved on the face or <tt>Optional.empty()</tt> if there is no data saved
     */
    <CF> Optional<CF> getData(@NotNull F face, @NotNull final String name, @NotNull final Class<CF> clazz);

    default boolean getBooleanData(@NotNull F face, @NotNull final String name) {
        return getData(face, name, Boolean.class).orElse(false);
    }

    default double getDoubleData(@NotNull F face, @NotNull final String name) {
        return getData(face, name, Double.class).orElse(0.0);
    }

    default int getIntegerData(@NotNull F face, @NotNull final String name) {
        return getData(face, name, Integer.class).orElse(0);
    }

    IMeshDataStorage<V, E, F> clone(IMesh<V, E, F> mesh);
}
