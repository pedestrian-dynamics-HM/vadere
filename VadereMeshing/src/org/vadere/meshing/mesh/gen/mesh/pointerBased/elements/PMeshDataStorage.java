package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

import java.util.Optional;

public class PMeshDataStorage implements IMeshDataStorage<PVertex, PHalfEdge, PFace> {
    private final IMesh<PVertex, PHalfEdge, PFace> mesh;

    public PMeshDataStorage(IMesh<PVertex, PHalfEdge, PFace> mesh) {
        this.mesh = mesh;
    }


    @Override
    public <CV> Optional<CV> getData(@NotNull final PVertex vertex, @NotNull final String name, @NotNull final Class<CV> clazz) {
        return Optional.ofNullable(vertex.getData(name, clazz));
    }

    @Override
    public <CV> void setData(@NotNull final PVertex vertex, @NotNull final String name, final CV data) {
        vertex.setData(name, data);
    }

    @Override
    public <CE> Optional<CE> getData(@NotNull final PHalfEdge edge, @NotNull final String name, @NotNull final Class<CE> clazz) {
        return Optional.ofNullable(edge.getData(name, clazz));
    }

    @Override
    public <CE> void setData(@NotNull final PHalfEdge edge, @NotNull final String name, @Nullable final CE data) {
        edge.setData(name, data);
    }

    @Override
    public <CF> Optional<CF> getData(@NotNull final PFace face, @NotNull final String name, @NotNull final Class<CF> clazz) {
        return Optional.ofNullable(face.getData(name, clazz));
    }

    @Override
    public IMeshDataStorage<PVertex, PHalfEdge, PFace> clone(IMesh<PVertex, PHalfEdge, PFace> mesh) {
        return new PMeshDataStorage(mesh);
    }

    @Override
    public <CF> void setData(@NotNull final PFace face, @NotNull final String name, @Nullable final CF data) {
        face.setData(name, data);
    }

    @Override
    public void clear() {
        // handled by the clear function of PMesh
    }
}
