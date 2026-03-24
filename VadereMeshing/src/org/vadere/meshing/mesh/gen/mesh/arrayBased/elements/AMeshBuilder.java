package org.vadere.meshing.mesh.gen.mesh.arrayBased.elements;

import org.vadere.meshing.mesh.gen.PolyConnectivity;
import org.vadere.meshing.mesh.gen.mesh.MeshBuilderBase;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.AMeshDataStorage;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.AMeshWithDataStorage;
import org.vadere.meshing.mesh.inter.mesh.builder.*;
import org.vadere.meshing.mesh.inter.meshConnectivity.IPolyConnectivity;

/**
 * Builder to create an array-based mesh
 *
 * Original author: Benedikt Zoennchen
 * Refactored by: Hayato Hess
 */
public class AMeshBuilder extends MeshBuilderBase<AVertex, AHalfEdge, AFace, AMeshDataStorage, AMesh> {
    private final AMeshBuilderOptimizer<AMeshVertices, AMeshEdges, AMeshFaces, AMesh> optimizer = new AMeshBuilderOptimizer<>(this);
    private final AMeshBuilderEdges<AMeshVertices, AMeshEdges, AMeshFaces, AMesh> edgesBuilder = new AMeshBuilderEdges<>(this);
    private final AMeshBuilderFaces<AMeshVertices, AMeshEdges, AMeshFaces, AMesh> facesBuilder = new AMeshBuilderFaces<>(this);
    private final AMeshBuilderVertices<AMeshVertices, AMeshEdges, AMeshFaces, AMesh> verticesBuilder = new AMeshBuilderVertices<>(this);
    private final PolyConnectivity<AVertex, AHalfEdge, AFace> connectivity = new PolyConnectivity<>(this);

    public AMeshBuilder(AMeshWithDataStorage toEdit) {
        super(toEdit);
    }

    public AMeshBuilder(){
        this(new AMesh());
    }

    private AMeshBuilder(AMesh mesh) {
        super(mesh, new AMeshDataStorage(mesh));
    }

    @Override
    public IMeshBuilder<AVertex, AHalfEdge, AFace> copy() {
        AMesh meshClone = getMesh().copy();
        return new AMeshBuilder(new AMeshWithDataStorage(meshClone, getDataStorage().clone(meshClone)));
    }

    @Override
    public IPolyConnectivity<AVertex, AHalfEdge, AFace> changeConnectivity() {
        return connectivity;
    }

    @Override
    public void clear() {
        getMesh().clear();
        getDataStorage().clear();
    }

    @Override
    public IMeshBuilder<AVertex, AHalfEdge, AFace> newInstance() {
        return new AMeshBuilder();
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
    public AMeshWithDataStorage getMeshWithDataStorage() {
        return new AMeshWithDataStorage(getMesh(), getDataStorage());
    }
}
