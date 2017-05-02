package org.vadere.util.geometry.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

/**
 * @author Benedikt Zoennchen
 */
public interface ITriEventListener<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> {

	void splitFaceEvent(F original, F... faces);

	void flipEdgeEvent(F f1, F f2);

	void insertEvent(E vertex);

	//void removeEvent()

	void deleteBoundaryFace(F face);
}
