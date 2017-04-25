package org.vadere.util.geometry.mesh.inter;

import org.vadere.util.geometry.mesh.triangulations.BasePointLocator;
import org.vadere.util.geometry.mesh.triangulations.DelaunayHierarchy;
import org.vadere.util.geometry.mesh.triangulations.DelaunayTree;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Collection;
import java.util.Optional;

/**
 * @author Benedikt Zoennchen
 *
 * @param <P>
 * @param <E>
 * @param <F>
 */
public interface IPointLocator<P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> extends ITriEventListener<P, E, F> {
	Collection<F> locatePoint(final IPoint point, final boolean insertion);

	Optional<F> locate(final IPoint point);

	static <P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> BasePointLocator<P, E, F> createBaseLocator(final ITriangulation<P, E, F> triangulation) {
		return new BasePointLocator<>(triangulation);
	}

	static <P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> DelaunayHierarchy<P, E, F> createDelaunayHierarchy(final ITriangulation<P, E, F> triangulation) {
		return new DelaunayHierarchy<>(triangulation);
	}

	static <P extends IPoint, E extends IHalfEdge<P>, F extends IFace<P>> DelaunayTree<P, E, F> createDelaunayTree(final ITriangulation<P, E, F> triangulation, final F superTriangle) {
		return new DelaunayTree<>(triangulation, superTriangle);
	}
}
