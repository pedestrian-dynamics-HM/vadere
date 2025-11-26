package org.vadere.meshing.mesh.gen.mesh.arrayBased.triangles;

import org.vadere.meshing.mesh.gen.mesh.arrayBased.elements.*;
import org.vadere.meshing.mesh.inter.mesh.IMesh;
import org.vadere.meshing.mesh.inter.mesh.triangle.ITriangleMeshFaces;

public class ATriangleMeshFaces extends AMeshFaces implements ITriangleMeshFaces<AVertex, AHalfEdge, AFace> {
    public ATriangleMeshFaces(IMesh<AVertex, AHalfEdge, AFace> mesh) {
        super(mesh);
    }

    /**
     * Copy constructor
     */
    public ATriangleMeshFaces(IMesh<AVertex, AHalfEdge, AFace> parent, ATriangleMeshFaces toCopy) {
        super(parent, toCopy);
    }
}
