package org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles;

import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.*;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshEdges;

public class PTriangleMeshEdges extends PMeshEdges implements ITriangleMeshEdges<PVertex, PHalfEdge, PFace> {
    public PTriangleMeshEdges(IMesh<PVertex, PHalfEdge, PFace> parent) {
        super(parent);
    }
}
