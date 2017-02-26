package org.vadere.util.triangulation;

import org.vadere.util.geometry.mesh.IFace;
import org.vadere.util.geometry.mesh.IHalfEdge;
import org.vadere.util.geometry.mesh.ITriConnectivity;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Set;
import java.util.stream.Stream;

public interface ITriangulation<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> extends Iterable<F>, ITriConnectivity<P, E, F> {
	void compute();
	Stream<F> streamFaces();
	Set<F> getFaces();
	E insert(final P point);
	void remove(final P point);
}
