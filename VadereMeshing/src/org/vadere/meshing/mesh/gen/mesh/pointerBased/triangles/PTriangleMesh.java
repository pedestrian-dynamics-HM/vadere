package org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.ReadOnlyTriangleConnectivity;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.*;
import org.vadere.meshing.mesh.inter.ITriEventListener;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyTriConnectivity;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;

import java.util.ArrayList;
import java.util.List;

public class PTriangleMesh extends PointerBasedMesh<PTriangleMeshVertices, PTriangleMeshEdges, PTriangleMeshFaces> implements ITriangleMesh<PVertex, PHalfEdge, PFace> {
    private final List<ITriEventListener<PVertex, PHalfEdge, PFace>> triEventListeners = new ArrayList<>();

    public PTriangleMesh() {
        super(  // parent is null as it will be set in super ctor
                new PTriangleMeshVertices(null),
                new PTriangleMeshEdges(null),
                new PTriangleMeshFaces(null));
    }

    public PTriangleMesh(PTriangleMesh meshToCopy) {
        super(meshToCopy,
                // parent is null as it will be set in super ctor
                new PTriangleMeshVertices(null),
                new PTriangleMeshEdges(null),
                new PTriangleMeshFaces(null));
    }

    @Override
    public IReadOnlyTriConnectivity<PVertex, PHalfEdge, PFace> readConnectivity() {
        return new ReadOnlyTriangleConnectivity<>(this);
    }

    @Override
    public synchronized PTriangleMesh copy() {
        return new PTriangleMesh(this);
    }

    @Override
    public void addTriEventListener(@NotNull ITriEventListener<PVertex, PHalfEdge, PFace> triEventListener) {
        triEventListeners.add(triEventListener);
    }

    @Override
    public void removeTriEventListener(@NotNull ITriEventListener<PVertex, PHalfEdge, PFace> triEventListener) {
        triEventListeners.remove(triEventListener);
    }

    @Override
    public void flipEdgeEvent(final PFace f1, final PFace f2) {
        for(ITriEventListener<PVertex, PHalfEdge, PFace> triEventListener : triEventListeners) {
            triEventListener.postFlipEdgeEvent(f1, f2);
        }
    }

    @Override
    public void splitTriangleEvent(final PFace original, final PFace f1, PFace f2, PFace f3, PVertex v) {
        for(ITriEventListener<PVertex, PHalfEdge, PFace> triEventListener : triEventListeners) {
            triEventListener.postSplitTriangleEvent(original, f1, f2, f3, v);
        }
    }

    @Override
    public void splitEdgeEvent(PHalfEdge originalEdge, PFace original, PFace f1, PFace f2, PVertex v) {
        for(ITriEventListener<PVertex, PHalfEdge, PFace> triEventListener : triEventListeners) {
            triEventListener.postSplitHalfEdgeEvent(originalEdge, original, f1, f2, v);
        }
    }

    @Override
    public void insertEvent(@NotNull final PHalfEdge halfEdge) {
        for(ITriEventListener<PVertex, PHalfEdge, PFace> triEventListener : triEventListeners) {
            triEventListener.postInsertEvent(vertices().getEndOf(halfEdge));
        }
    }
}
