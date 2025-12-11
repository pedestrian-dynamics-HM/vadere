package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.MeshBase;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

import java.util.HashMap;
import java.util.Map;

public abstract class PointerBasedMesh<
        Vertices extends PMeshVertices,
        Edges extends PMeshEdges,
        Faces extends PMeshFaces>
        extends MeshBase<PVertex, PHalfEdge, PFace, Vertices, Edges, Faces> implements IMesh<PVertex, PHalfEdge, PFace> {

    @Override
    public void clear() {
        faces().clear();
        edges().clear();
        vertices().clear();
    }

    protected PointerBasedMesh(Vertices vertices, Edges edges, Faces faces) {
        super(vertices, edges, faces);

        vertices().parent = this;
        edges().parent = this;
        faces().parent = this;
        clear();
    }

    /**
     * Copy Constructor
     */
    protected PointerBasedMesh(PointerBasedMesh<Vertices, Edges, Faces> meshToCopy, Vertices vertices, Edges edges, Faces faces) {
        super(vertices, edges, faces);

        vertices().parent = this;
        edges().parent = this;
        faces().parent = this;

        copyPointerStructure(meshToCopy);
    }

    @Override
    public IMeshDataStorage<PVertex, PHalfEdge, PFace> createEmptyDataStorage() {
        return new PMeshDataStorage(this);
    }

    private void copyPointerStructure(PointerBasedMesh<Vertices, Edges, Faces> meshToCopy) {
        Map<PVertex, PVertex> vertexMap = new HashMap<>();
        Map<PHalfEdge, PHalfEdge> edgeMap = new HashMap<>();
        Map<PFace, PFace> faceMap = new HashMap<>();

        vertices().beginCopyFrom(meshToCopy, vertexMap);
        edges().beginCopyFrom(meshToCopy, vertexMap, edgeMap);

        faces().copyFrom(meshToCopy, faceMap, edgeMap);

        vertices().finalizeCopy(edgeMap);
        edges().finalizeCopy(faceMap, edgeMap);
    }
}
