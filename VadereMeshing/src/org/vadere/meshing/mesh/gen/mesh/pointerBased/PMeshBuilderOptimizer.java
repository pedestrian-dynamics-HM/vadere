package org.vadere.meshing.mesh.gen.mesh.pointerBased;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.inter.mesh.builder.IMeshOptimizer;
import org.vadere.util.logging.Logger;

import java.util.stream.Collectors;

public class PMeshBuilderOptimizer implements IMeshOptimizer<PVertex, PHalfEdge, PFace> {
    private static final Logger logger = Logger.getLogger(PMeshBuilderOptimizer.class);

    private final PMeshBuilder parent;

    public PMeshBuilderOptimizer(PMeshBuilder parent) {
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
        parent.mesh.faces = parent.mesh.faces.stream().filter(f -> !parent.mesh.isDestroyed(f)).collect(Collectors.toList());
        parent.mesh.edges = parent.mesh.edges.stream().filter(e -> !parent.mesh.isDestroyed(e)).collect(Collectors.toList());
        parent.mesh.vertices = parent.mesh.vertices.stream().filter(v -> !parent.mesh.isDestroyed(v)).collect(Collectors.toList());
    }
}
