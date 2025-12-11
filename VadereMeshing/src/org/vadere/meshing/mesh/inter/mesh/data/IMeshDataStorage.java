package org.vadere.meshing.mesh.inter.mesh.data;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.*;
import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

import java.util.List;

/**
 * The IMeshDataStorage interface provides mechanisms for accessing and managing data
 * associated with vertices, edges, and faces within a mesh structure. It extends the
 * functionality of both IMeshDataStorageReader and IMeshDataStorageWriter to enable
 * data reading and writing operations.
 */
public interface IMeshDataStorage<V extends IVertex, E extends IHalfEdge, F extends IFace>
        extends IMeshDataStorageReader<V,E,F>, IMeshDataStorageWriter<V, E, F> {

    default IEdgeContainerDouble<V, E, F> getDoubleEdgeContainer(@NotNull final String name) {
        return new IEdgeContainerDouble<>() {
            @Override
            public double getValue(@NotNull final E edge) {
                return getDoubleData(edge, name);
            }

            @Override
            public void setValue(@NotNull final E edge, double value) {
                setDoubleData(edge, name, value);
            }
        };
    }

    default IVertexContainerDouble<V, E, F> getDoubleVertexContainer(@NotNull final String name, IMesh<V, E, F> mesh) {
        return new IVertexContainerDouble<>() {
            @Override
            public double getValue(@NotNull V vertex) {
                return getDoubleData(vertex, name);
            }

            @Override
            public void setValue(@NotNull V vertex, double value) {
                setDoubleData(vertex, name, value);
            }

            @Override
            public void reset() {
                for(V v : mesh.vertices().getAll()) {
                    setValue(v, 0.0);
                }
            }
        };
    }

    default IVertexContainerBoolean<V, E, F> getBooleanVertexContainer(@NotNull final String name) {
        return new IVertexContainerBoolean<>() {
            @Override
            public boolean getValue(@NotNull final V vertex) {
                return getBooleanData(vertex, name);
            }

            @Override
            public void setValue(@NotNull final V vertex, final boolean value) {
                setBooleanData(vertex, name, value);
            }
        };
    }

    default <CV> IVertexContainerObject<V, E, F, CV> getObjectVertexContainer(@NotNull final String name, final Class<CV> clazz) {

        return new IVertexContainerObject<>() {

            @Override
            public CV getValue(@NotNull final V v) {
                return getData(v, name, clazz).get();
            }

            @Override
            public void setValue(@NotNull final V v, final CV value) {
                setData(v, name, value);
            }

        };
    }

    default IEdgeContainerBoolean<V, E, F> getBooleanEdgeContainer(@NotNull final String name) {
        return new IEdgeContainerBoolean<>() {
            @Override
            public boolean getValue(@NotNull final E edge) {
                return getBooleanData(edge, name);
            }

            @Override
            public void setValue(@NotNull final E edge, final boolean value) {
                setBooleanData(edge, name, value);
            }
        };
    }

    default <CV> IEdgeContainerObject<V, E, F, CV> getObjectEdgeContainer(@NotNull final String name, final Class<CV> clazz) {

        return new IEdgeContainerObject<>() {

            @Override
            public CV getValue(@NotNull final E edge) {
                return getData(edge, name, clazz).get();
            }

            @Override
            public void setValue(@NotNull final E edge, final CV value) {
                setData(edge, name, value);
            }

        };
    }

}
