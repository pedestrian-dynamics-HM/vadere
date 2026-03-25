package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshOptimizer;
import org.vadere.util.logging.Logger;

import java.util.stream.Collectors;

public class PMeshBuilderOptimizer<Vertices extends PMeshVertices, Edges extends PMeshEdges, Faces extends PMeshFaces,
        Mesh extends PointerBasedMesh<Vertices,Edges,Faces>> implements IMeshOptimizer<PVertex, PHalfEdge, PFace> {
    private static final Logger logger = Logger.getLogger(PMeshBuilderOptimizer.class);

    private final MeshBuilderBase<PVertex, PHalfEdge, PFace, PMeshDataStorage, Mesh> parent;

    public PMeshBuilderOptimizer(MeshBuilderBase<PVertex, PHalfEdge, PFace, PMeshDataStorage, Mesh> parent) {
        this.parent = parent;
    }

    @Override
    public void arrangeMemory(@NotNull Iterable<PFace> faceOrder) {
        try {
            throw new UnsupportedOperationException("not jet implemented.");
        } catch (UnsupportedOperationException e) {
            logger.warn(e.getMessage());
        }
    }

    @Override
    public void garbageCollection() {
        parent.getMesh().faces().items = parent.getMesh().faces().stream().filter(f -> !parent.getMesh().faces().isDestroyed(f)).collect(Collectors.toList());
        parent.getMesh().edges().items = parent.getMesh().edges().stream().filter(e -> !parent.getMesh().edges().isDestroyed(e)).collect(Collectors.toList());
        parent.getMesh().vertices().items = parent.getMesh().vertices().stream().filter(v -> !parent.getMesh().vertices().isDestroyed(v)).collect(Collectors.toList());
    }
}
