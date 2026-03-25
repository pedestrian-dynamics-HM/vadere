package org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles;

import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.gen.TriangleConnectivity;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.*;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.*;
import org.vadere.meshing.mesh.inter.mesh.ITriangleMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.*;
import org.vadere.meshing.mesh.inter.meshConnectivity.ITriConnectivity;

public class ATriangleMeshBuilder extends MeshBuilderBase<AVertex, AHalfEdge, AFace, AMeshDataStorage, ATriangleMesh> implements ITriangleMeshBuilder<AVertex, AHalfEdge, AFace> {
    private final AMeshBuilderOptimizer<ATriangleMeshVertices, ATriangleMeshEdges, ATriangleMeshFaces, ATriangleMesh> optimizer = new AMeshBuilderOptimizer<>(this);
    private final AMeshBuilderEdges<ATriangleMeshVertices, ATriangleMeshEdges, ATriangleMeshFaces, ATriangleMesh> edgesBuilder = new AMeshBuilderEdges<>(this);
    private final AMeshBuilderFaces<ATriangleMeshVertices, ATriangleMeshEdges, ATriangleMeshFaces, ATriangleMesh> facesBuilder = new AMeshBuilderFaces<>(this);
    private final AMeshBuilderVertices<ATriangleMeshVertices, ATriangleMeshEdges, ATriangleMeshFaces, ATriangleMesh> verticesBuilder = new AMeshBuilderVertices<>(this);
    private final TriangleConnectivity<AVertex, AHalfEdge, AFace> connectivity = new TriangleConnectivity<>(this);

    public ATriangleMeshBuilder(ATriangleMeshWithDataStorage toEdit) {
        super(toEdit);
    }

    public ATriangleMeshBuilder(){
        this(new ATriangleMesh());
    }

    private ATriangleMeshBuilder(ATriangleMesh mesh) {
        super(mesh, new AMeshDataStorage(mesh));
    }

    @Override
    public ITriangleMeshBuilder<AVertex, AHalfEdge, AFace> copy() {
        ATriangleMesh meshClone = getMesh().copy();
        return new ATriangleMeshBuilder(new ATriangleMeshWithDataStorage(meshClone, getDataStorage().clone(meshClone)));
    }

    @Override
    public ITriangleMeshBuilder<AVertex, AHalfEdge, AFace> newInstance() {
        return new ATriangleMeshBuilder();
    }

    @Override
    public void clear() {
        getMesh().clear();
        getDataStorage().clear();
    }

    @Override
    public IMeshBuilderEdges<AVertex, AHalfEdge, AFace> edges() {
        return edgesBuilder;
    }

    @Override
    public IMeshBuilderVertices<AVertex, AHalfEdge, AFace> vertices() {
        return verticesBuilder;
    }

    @Override
    public IMeshBuilderFaces<AVertex, AHalfEdge, AFace> faces() {
        return facesBuilder;
    }

    @Override
    public IMeshOptimizer<AVertex, AHalfEdge, AFace> getOptimizer() {
        return optimizer;
    }

    @Override
    public ITriangleMeshWithDataStorage<AVertex, AHalfEdge, AFace> getMeshWithDataStorage() {
        return new ATriangleMeshWithDataStorage(getMesh(), getDataStorage());
    }

    @Override
    public ITriConnectivity<AVertex, AHalfEdge, AFace> changeConnectivity() {
        return connectivity;
    }
}
