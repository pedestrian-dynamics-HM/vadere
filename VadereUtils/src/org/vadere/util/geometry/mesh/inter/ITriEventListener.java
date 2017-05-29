package org.vadere.util.geometry.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

/**
 * @author Benedikt Zoennchen
 */
public interface ITriEventListener<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {

	void splitTriangleEvent(F original, F f1, F f2, F f3);

	void splitEdgeEvent(F original, F f1, F f2);

	void flipEdgeEvent(F f1, F f2);

	void insertEvent(V vertex);

	//void removeEvent()

	void deleteBoundaryFace(F face);
}
