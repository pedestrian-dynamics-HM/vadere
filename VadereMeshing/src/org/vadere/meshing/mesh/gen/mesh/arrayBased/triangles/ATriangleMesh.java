package org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.ReadOnlyTriangleConnectivity;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.ArrayBasedMesh;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.*;
import org.vadere.meshing.mesh.inter.ITriEventListener;
import org.vadere.meshing.mesh.inter.meshConnectivity.IReadOnlyTriConnectivity;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMesh;

import java.util.ArrayList;
import java.util.List;

public class ATriangleMesh extends ArrayBasedMesh<ATriangleMeshVertices, ATriangleMeshEdges, ATriangleMeshFaces>
        implements ITriangleMesh<AVertex, AHalfEdge, AFace> {

    private final List<ITriEventListener<AVertex, AHalfEdge, AFace>> triEventListeners = new ArrayList<>();

    public ATriangleMesh() {
        super(  // parent is null as it will be set in super ctor
                new ATriangleMeshVertices(null),
                new ATriangleMeshEdges(null),
                new ATriangleMeshFaces(null));
    }

    public ATriangleMesh(ATriangleMesh meshToCopy) {
        super(meshToCopy,
                new ATriangleMeshVertices(null, meshToCopy.vertices()),
                new ATriangleMeshEdges(null, meshToCopy.edges()),
                new ATriangleMeshFaces(null, meshToCopy.faces()));
    }

    @Override
    public IReadOnlyTriConnectivity<AVertex, AHalfEdge, AFace> readConnectivity() {
        return new ReadOnlyTriangleConnectivity<>(this);
    }

    @Override
    public ATriangleMesh copy() {
        return new ATriangleMesh(this);
    }

    @Override
    public void addTriEventListener(@NotNull ITriEventListener<AVertex, AHalfEdge, AFace> triEventListener) {
        triEventListeners.add(triEventListener);
    }

    @Override
    public void removeTriEventListener(@NotNull ITriEventListener<AVertex, AHalfEdge, AFace> triEventListener) {
        triEventListeners.remove(triEventListener);
    }

    @Override
    public void flipEdgeEvent(final AFace f1, final AFace f2) {
        for(ITriEventListener<AVertex, AHalfEdge, AFace> triEventListener : triEventListeners) {
            triEventListener.postFlipEdgeEvent(f1, f2);
        }
    }

    @Override
    public void splitTriangleEvent(final AFace original, final AFace f1, AFace f2, AFace f3, AVertex v) {
        for(ITriEventListener<AVertex, AHalfEdge, AFace> triEventListener : triEventListeners) {
            triEventListener.postSplitTriangleEvent(original, f1, f2, f3, v);
        }
    }

    @Override
    public void splitEdgeEvent(AHalfEdge originalEdge, AFace original, AFace f1, AFace f2, AVertex v) {
        for(ITriEventListener<AVertex, AHalfEdge, AFace> triEventListener : triEventListeners) {
            triEventListener.postSplitHalfEdgeEvent(originalEdge, original, f1, f2, v);
        }
    }

    @Override
    public void insertEvent(@NotNull final AHalfEdge halfEdge) {
        for(ITriEventListener<AVertex, AHalfEdge, AFace> triEventListener : triEventListeners) {
            triEventListener.postInsertEvent(vertices().getEndOf(halfEdge));
        }
    }
}
