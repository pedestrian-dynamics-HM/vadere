package org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles;

import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.*;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshEdges;

public class ATriangleMeshEdges extends AMeshEdges implements ITriangleMeshEdges<AVertex, AHalfEdge, AFace> {
    public ATriangleMeshEdges(IMesh<AVertex, AHalfEdge, AFace> parent) {
        super(parent);
    }

    /**
     * Copy constructor
     */
    public ATriangleMeshEdges(IMesh<AVertex, AHalfEdge, AFace> parent, ATriangleMeshEdges toCopy) {
        super(parent, toCopy);
    }
}
