package org.vadere.meshing.mesh.triangulation.triangulator.inter;

import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IVertex;

import java.util.Collection;
import java.util.Collections;

public interface IRefiner<V extends IVertex, E extends IHalfEdge, F extends IFace> extends ITriangulator<V, E, F> {

	void refine();

	boolean isFinished();

	default Collection<V> getFixPoints() {
		return Collections.EMPTY_LIST;
	}
}
