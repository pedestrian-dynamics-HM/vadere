package org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles;

import org.vadere.meshing.mesh.gen.TriangleConnectivity;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AFace;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AVertex;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles.ATriangleMesh;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles.ATriangleMeshEdges;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles.ATriangleMeshFaces;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles.ATriangleMeshVertices;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.*;
import org.vadere.meshing.mesh.inter.mesh.ITriangleMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.*;
import org.vadere.meshing.mesh.inter.meshConnectivity.ITriConnectivity;

public class PTriangleMeshBuilder extends MeshBuilderBase<PVertex, PHalfEdge, PFace, PMeshDataStorage, PTriangleMesh> implements ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> {
    private final PMeshBuilderOptimizer<PTriangleMeshVertices, PTriangleMeshEdges, PTriangleMeshFaces, PTriangleMesh> optimizer = new PMeshBuilderOptimizer<>(this);
    private final PMeshBuilderEdges<PTriangleMeshVertices, PTriangleMeshEdges, PTriangleMeshFaces, PTriangleMesh> edgesBuilder = new PMeshBuilderEdges<>(this);
    private final PMeshBuilderFaces<PTriangleMeshVertices, PTriangleMeshEdges, PTriangleMeshFaces, PTriangleMesh> facesBuilder = new PMeshBuilderFaces<>(this);
    private final PMeshBuilderVertices<PTriangleMeshVertices, PTriangleMeshEdges, PTriangleMeshFaces, PTriangleMesh> verticesBuilder = new PMeshBuilderVertices<>(this);
    private final TriangleConnectivity<PVertex, PHalfEdge, PFace> connectivity = new TriangleConnectivity<>(this);

    public PTriangleMeshBuilder(PTriangleMeshWithDataStorage toEdit) {
        super(toEdit);
    }

    public PTriangleMeshBuilder(){
        this(new PTriangleMesh());
    }

    private PTriangleMeshBuilder(PTriangleMesh mesh) {
        super(mesh, new PMeshDataStorage(mesh));
    }

    @Override
    public ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> copy() {
        PTriangleMesh meshClone = getMesh().copy();
        return new PTriangleMeshBuilder(new PTriangleMeshWithDataStorage(meshClone, getDataStorage().clone(meshClone)));
    }

    @Override
    public void clear() {
        getMesh().clear();
        getDataStorage().clear();
    }

    @Override
    public ITriangleMeshBuilder<PVertex, PHalfEdge, PFace> newInstance() {
        return new PTriangleMeshBuilder();
    }

    @Override
    public IMeshBuilderEdges<PVertex, PHalfEdge, PFace> edges() {
        return edgesBuilder;
    }

    @Override
    public IMeshBuilderVertices<PVertex, PHalfEdge, PFace> vertices() {
        return verticesBuilder;
    }

    @Override
    public IMeshBuilderFaces<PVertex, PHalfEdge, PFace> faces() {
        return facesBuilder;
    }

    @Override
    public IMeshOptimizer<PVertex, PHalfEdge, PFace> getOptimizer() {
        return optimizer;
    }

    @Override
    public ITriangleMeshWithDataStorage<PVertex, PHalfEdge, PFace> getMeshWithDataStorage() {
        return new PTriangleMeshWithDataStorage(getMesh(), getDataStorage());
    }

    @Override
    public ITriConnectivity<PVertex, PHalfEdge, PFace> changeConnectivity() {
        return connectivity;
    }

}
