package org.vadere.meshing.mesh.inter.mesh.triangle;

import org.vadere.meshing.mesh.inter.mesh.IFace;
import org.vadere.meshing.mesh.inter.mesh.IHalfEdge;
import org.vadere.meshing.mesh.inter.mesh.IMeshEdges;
import org.vadere.meshing.mesh.inter.mesh.IVertex;

public interface ITriangleMeshEdges<V extends IVertex, E extends IHalfEdge, F extends IFace> extends IMeshEdges<V, E, F> {

}
