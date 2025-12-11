package org.vadere.meshing.mesh.gen.mesh.arrayBased.elements;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.gen.mesh.MeshBase;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.AMeshDataStorage;
import org.vadere.meshing.mesh.inter.mesh.data.IMeshDataStorage;

/**
 * Original author: Benedikt Zoennchen
 * Refactored by: Hayato Hess
 */
public abstract class ArrayBasedMesh<
        Vertices extends AMeshVertices,
        Edges extends AMeshEdges,
        Faces extends AMeshFaces>
        extends MeshBase<AVertex, AHalfEdge, AFace, Vertices, Edges, Faces> {

    protected ArrayBasedMesh(Vertices vertices, Edges edges, Faces faces) {
        super(vertices, edges, faces);
        vertices().parent = this;
        edges().parent = this;
        faces().parent = this;
        this.elementRemoved = false;
        clear();
    }

    /**
     * Copy Constructor
     */
    protected ArrayBasedMesh(ArrayBasedMesh<Vertices, Edges, Faces> meshToCopy, Vertices vertices, Edges edges, Faces faces) {
        super(vertices, edges, faces);
        vertices().parent = this;
        edges().parent = this;
        faces().parent = this;
        this.elementRemoved = meshToCopy.elementRemoved;
    }

    private boolean elementRemoved;

    @Override
    public IMeshDataStorage<AVertex, AHalfEdge, AFace> createEmptyDataStorage() {
        return new AMeshDataStorage(this);
    }

    public boolean isElementRemoved() {
        return elementRemoved;
    }

    public void setElementRemoved(boolean elementRemoved) {
        this.elementRemoved = elementRemoved;
    }

    public void clear() {
        faces().clear();
        edges().clear();
        vertices().clear();
        setElementRemoved(false);
    }
}
