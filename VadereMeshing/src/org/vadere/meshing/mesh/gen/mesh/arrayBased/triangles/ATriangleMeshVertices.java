package org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles;

import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.*;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshVertices;

public class ATriangleMeshVertices extends AMeshVertices implements ITriangleMeshVertices<AVertex, AHalfEdge, AFace> {
    public ATriangleMeshVertices(IMesh<AVertex, AHalfEdge, AFace> mesh) {
        super(mesh);
    }

    /**
     * Copy constructor
     */
    public ATriangleMeshVertices(IMesh<AVertex, AHalfEdge, AFace> parent, ATriangleMeshVertices toCopy) {
        super(parent, toCopy);
    }
}
