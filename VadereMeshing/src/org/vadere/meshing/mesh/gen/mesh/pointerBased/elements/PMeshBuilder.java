package org.vadere.meshing.mesh.gen.mesh.pointerBased.elements;

import org.vadere.meshing.mesh.gen.PolyConnectivity;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AMesh;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AMeshEdges;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AMeshFaces;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AMeshVertices;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.PMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.IMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.*;
import org.vadere.meshing.mesh.inter.meshConnectivity.IPolyConnectivity;

public class PMeshBuilder extends MeshBuilderBase<PVertex, PHalfEdge, PFace, PMeshDataStorage, PMesh> {
    private final PMeshBuilderEdges<PMeshVertices, PMeshEdges, PMeshFaces, PMesh> edgesBuilder = new PMeshBuilderEdges<>(this);
    private final PMeshBuilderVertices<PMeshVertices, PMeshEdges, PMeshFaces, PMesh> verticesBuilder = new PMeshBuilderVertices<>(this);
    private final PMeshBuilderFaces<PMeshVertices, PMeshEdges, PMeshFaces, PMesh> facesBuilder = new PMeshBuilderFaces<>(this);
    private final PolyConnectivity<PVertex, PHalfEdge, PFace> connectivity = new PolyConnectivity<>(this);

    public PMeshBuilder(PMeshWithDataStorage toEdit) {
        super(toEdit);
    }

    public PMeshBuilder() {
        this(new PMesh());
    }

    private PMeshBuilder(PMesh mesh){
        super(mesh, new PMeshDataStorage(mesh));
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
        return new PMeshBuilderOptimizer<>(this);
    }

    @Override
    public IMeshWithDataStorage<PVertex, PHalfEdge, PFace> getMeshWithDataStorage() {
        return new PMeshWithDataStorage(getMesh(), getDataStorage());
    }

    @Override
    public IMeshBuilder<PVertex, PHalfEdge, PFace> copy() {
        PMesh meshClone = getMesh().copy();
        return new PMeshBuilder(new PMeshWithDataStorage(meshClone, getDataStorage().clone(meshClone)));
    }

    @Override
    public IPolyConnectivity<PVertex, PHalfEdge, PFace> changeConnectivity() {
        return connectivity;
    }

    @Override
    public void clear() {
        getMesh().clear();
        getDataStorage().clear();
    }

    @Override
    public IMeshBuilder<PVertex, PHalfEdge, PFace> newInstance() {
        return new PMeshBuilder();
    }
}
