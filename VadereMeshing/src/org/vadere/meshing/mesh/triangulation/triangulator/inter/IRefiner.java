package org.vadere.meshing.mesh.triangulation.triangulator.inter;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.util.geometry.shapes.IPoint;

public interface IRefiner<P extends IPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> extends ITriangulator<P, CE, CF, V, E, F> {

	void refine();
	boolean isFinished();

}
