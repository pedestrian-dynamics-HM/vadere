package org.vadere.util.geometry.mesh.impl;

import org.vadere.util.geometry.mesh.gen.PFace;
import org.vadere.util.geometry.mesh.gen.PHalfEdge;
import org.vadere.util.geometry.mesh.gen.PVertex;
import org.vadere.util.geometry.mesh.inter.ITriangulationSupplier;
import org.vadere.util.geometry.mesh.triangulation.triangulator.UniformRefinementTriangulator;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.mesh.triangulation.adaptive.IEdgeLengthFunction;

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
