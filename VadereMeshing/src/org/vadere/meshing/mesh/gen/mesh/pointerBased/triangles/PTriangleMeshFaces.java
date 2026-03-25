package org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles;

import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.*;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshFaces;

public class PTriangleMeshFaces extends PMeshFaces implements ITriangleMeshFaces<PVertex, PHalfEdge, PFace> {
    public PTriangleMeshFaces(IMesh<PVertex, PHalfEdge, PFace> parent) {
        super(parent);
    }
}
