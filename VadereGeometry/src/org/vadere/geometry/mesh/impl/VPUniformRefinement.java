package org.vadere.geometry.mesh.impl;

import org.vadere.geometry.mesh.gen.PFace;
import org.vadere.geometry.mesh.gen.PHalfEdge;
import org.vadere.geometry.mesh.gen.PVertex;
import org.vadere.geometry.mesh.inter.ITriangulationSupplier;
import org.vadere.geometry.mesh.triangulation.triangulator.UniformRefinementTriangulator;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VRectangle;
import org.vadere.geometry.shapes.VShape;
import org.vadere.geometry.mesh.triangulation.adaptive.IEdgeLengthFunction;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class VPUniformRefinement extends UniformRefinementTriangulator<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> {
	public VPUniformRefinement(
			final ITriangulationSupplier<VPoint, PVertex<VPoint>, PHalfEdge<VPoint>, PFace<VPoint>> supplier,
			final VRectangle bound,
			final Collection<VShape> boundary,
			final IEdgeLengthFunction lenFunc) {
		super(supplier, bound, boundary, lenFunc, p -> -1.0);
	}
}
