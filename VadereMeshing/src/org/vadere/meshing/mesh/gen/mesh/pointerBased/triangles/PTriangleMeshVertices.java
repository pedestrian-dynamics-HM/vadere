package org.vadere.meshing.mesh.gen.mesh.pointerBased.triangles;

import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AFace;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AHalfEdge;
import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.AVertex;
import org.vadere.meshing.mesh.gen.mesh.pointerBased.elements.*;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshVertices;

public class PTriangleMeshVertices extends PMeshVertices implements ITriangleMeshVertices<PVertex, PHalfEdge, PFace> {
    public PTriangleMeshVertices(IMesh<PVertex, PHalfEdge, PFace> parent) {
        super(parent);
    }
}
